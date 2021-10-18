package uniregistrar.driver.did.btcr.service;

import info.weboftrust.btctxlookup.Chain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Produces new UTXOs via sending them to UTXO wallet from the server wallet
 * TODO: UTXO Wallet requires key cleanup in case of failure to remove imported
 * keys before the completion step
 */
public class UTXOProducer {

	private final Logger log = LogManager.getLogger(UTXOProducer.class);
	private final ScheduledExecutorService executor;
	private final int targetUtxoUpkeep;
	private final Coin targetUtxoValue;
	private final NetworkParameters params;
	private final Script.ScriptType scriptType = Script.ScriptType.P2PKH;
	private final DidBtcrDriver driver;
	private final Chain chain;
	private final Wallet serverWallet;
	private final Wallet utxoWallet;

	private static final int CHECK_INTERVAL = 150;

	public UTXOProducer(DidBtcrDriver driver, Chain chain, Wallet serverWallet, @Nullable Integer targetUtxoUpkeep,
	                    @Nullable Coin targetUtxoValue) {

		this.driver = driver;
		this.chain = chain;
		this.serverWallet = serverWallet;
		this.utxoWallet = driver.getUtxoWallet(chain);
		this.params = BitcoinUtils.chainToNetworkParameters(chain);
		this.targetUtxoUpkeep = targetUtxoUpkeep == null ? DriverConstants.TARGET_UTXO_UPKEEP : targetUtxoUpkeep;
		this.targetUtxoValue = targetUtxoValue == null ? DriverConstants.TARGET_UTXO_VALUE : targetUtxoValue;
		this.executor = ExecutorProvider.getSingleThreadScheduledExecutor(this.getClass().getSimpleName());
	}

	private long checkSuitableUtxos() {
		Context.propagate(driver.getContext(chain));
		long utxoCount = utxoWallet
				.calculateAllSpendCandidates(false, true).stream().filter(TransactionOutput::isAvailableForSpending).count();
		log.debug("Current suitable utxo amount: {}", utxoCount);
		return utxoCount;
	}

	public void start() {
		while (!driver.isOnline()) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e);
				stop();
			}
		}

		log.debug("Starting the UTXO provider service...");

		serverWallet.addCoinsReceivedEventListener((w, tx, prevBalance, newBalance) -> {
			Context.propagate(driver.getContext(chain));
			log.debug("Received new transaction: {}", tx::getTxId);
			log.debug("New balance is: {}", newBalance::toPlainString);
		});
		executor.execute(() -> {
			try {
				splitUtxos();
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				stop();
			}
		});
	}

	private void sleep(int seconds) throws InterruptedException {
		TimeUnit.SECONDS.sleep(seconds);
	}

	private void splitUtxos() throws InterruptedException {

		Context.propagate(driver.getContext(chain));

		while (driver.isOnline()) {
			sleep(CHECK_INTERVAL);
			if (checkSuitableUtxos() >= targetUtxoUpkeep) {
				continue;
			}

			log.debug("Split UTXOs...");

			Coin balance = serverWallet.getBalance();
			log.debug("Server balance: {}", balance::toFriendlyString);
			if (serverWallet.getBalance().isLessThan(targetUtxoValue.multiply(2))) {
				log.debug("Server balance is too low! Skipping splitting UTXOs");
				continue;
			}

			int maxOutput = DriverConstants.OUTPUT_COUNT_PER_TX; // To prevent excessive TXs
			Transaction tx = new Transaction(params);
			Coin currentVal = balance.subtract(DriverConstants.DEFAULT_TX_FEE_KB);
			List<ECKey> keys = new ArrayList<>();
			while (currentVal.isGreaterThan(targetUtxoValue) && maxOutput-- > 0) {
				ECKey key = new ECKey();
				keys.add(key);
				utxoWallet.importKey(key);
				tx.addOutput(targetUtxoValue, Address.fromKey(params, key, scriptType));

				currentVal = currentVal.subtract(targetUtxoValue);
			}
			if(tx.getOutputs().isEmpty()){
				log.debug("Nothing to split, skipping..");
				continue;
			}
			TransactionBroadcast sent;
			try {
				sent = driver.getWalletService(chain).completeAndSendTransaction(tx);
				if (sent == null) {
					log.debug("Cannot send the TX, skipping..");
					continue;
				}
				Transaction bTx = sent.future().get();
				TransactionConfidence.ConfidenceType confidenceType = null;
				log.debug("TX confidence: {}", bTx::getConfidence);
				while ((confidenceType = bTx.getConfidence().getConfidenceType()) != TransactionConfidence.ConfidenceType.BUILDING) {
					if (confidenceType != TransactionConfidence.ConfidenceType.PENDING) {
						log.error("Transaction broadcasting error: {}", bTx::getTxId);
						keys.forEach(utxoWallet::removeKey);
						continue;
					}
					TimeUnit.SECONDS.sleep(5);
				}
				log.debug("TX confidence: {}", bTx::getConfidence);
			} catch (InsufficientMoneyException | ExecutionException  e) {
				log.error(e.getMessage());
				keys.forEach(utxoWallet::removeKey);
			}
		}

	}

	public void stop() {
		log.info("UTXO Producer is stopping...");
		executor.shutdown();
	}
}
