package uniregistrar.driver.did.btcr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;

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
	// Ready to use UTXO count
	private final AtomicLong readyUtxoCount;
	private final NetworkParameters params;
	private final Script.ScriptType scriptType = Script.ScriptType.P2PKH;
	private final DidBtcrDriver driver;
	private final Chain chain;
	private final Wallet serverWallet;
	private final Wallet utxoWallet;

	private final ReentrantLock lock = new ReentrantLock();

	public UTXOProducer(DidBtcrDriver driver, Chain chain, Wallet serverWallet, @Nullable Integer targetUtxoUpkeep,
			@Nullable Coin targetUtxoValue) {

		this.driver = driver;
		this.chain = chain;
		this.serverWallet = serverWallet;
		this.utxoWallet = driver.getUtxoWallet(chain);
		this.params = BitcoinUtils.chainToNetworkParameters(chain);
		this.targetUtxoUpkeep = targetUtxoUpkeep == null ? DriverConstants.TARGET_UTXO_UPKEEP : targetUtxoUpkeep;
		this.targetUtxoValue = targetUtxoValue == null ? DriverConstants.TARGET_UTXO_VALUE : targetUtxoValue;
		this.readyUtxoCount = new AtomicLong();
		this.readyUtxoCount.set(checkSuitableUtxos());
		this.executor = ExecutorProvider.getSingleThreadScheduledExecutor(this.getClass().getSimpleName());
	}

	private long checkSuitableUtxos() {
		Context.propagate(driver.getContext(chain));
		long utxoCount = utxoWallet.calculateAllSpendCandidates(false, true).size();
		log.debug("Current suitable utxo amount: {}", utxoCount);
		return utxoCount;
	}

	public void start() {
		while (!driver.isOnline()) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e);
			}
		}

		log.debug("Starting the UTXO provider service...");

		readyUtxoCount.set(checkSuitableUtxos());

		utxoWallet.addChangeEventListener(wallet -> {
			Context.propagate(driver.getContext(chain));
			readyUtxoCount.set(checkSuitableUtxos());
			if (readyUtxoCount.get() < targetUtxoUpkeep) {
				log.debug("UTXOWallet: Available UTXOs are less than the target!");
				if (!lock.isLocked()) {
					CompletableFuture.runAsync(this::splitUtxos);
				} else {
					log.debug("Splitting process is locked by another thread, skipping...");
				}
			}
		});

		serverWallet.addCoinsReceivedEventListener((w, tx, prevBalance, newBalance) -> {
			Context.propagate(driver.getContext(chain));
			if (!driver.isOnline()) {
				return;
			}
			log.debug("Received new transaction: {}", tx::getTxId);
			log.debug("New balance is: {}", newBalance::toPlainString);
			if (readyUtxoCount.get() < targetUtxoUpkeep) {
				log.debug("ServerWallet: Available UTXOs are less than the target!");
				if (!lock.isLocked()) {
					CompletableFuture.runAsync(this::splitUtxos);
				} else {
					log.debug("Splitting process is locked by another thread, skipping...");
				}
			}
		});

		if (readyUtxoCount.get() < targetUtxoUpkeep) {
			log.debug("Starting to prepare UTXOs...");
			executor.schedule(this::splitUtxos, 10, TimeUnit.SECONDS);
		}
	}

	private void splitUtxos() {
		lock.lock();
		try {
			log.debug("Split UTXOs...");
			if (readyUtxoCount.get() >= targetUtxoUpkeep) {
				return;
			}

			readyUtxoCount.set(checkSuitableUtxos());
			if (readyUtxoCount.get() >= targetUtxoUpkeep) {
				return;
			}

			if (!driver.isOnline()) {
				stop();
				return;
			}

			Context.propagate(driver.getContext(chain));
			List<TransactionOutput> serverUtxos = serverWallet.calculateAllSpendCandidates(true, true);
			if (serverUtxos.size() == 0) {
				log.debug("Server has no UTXO to split!");
				return;
			}
			log.debug("Server UTXO count: {}", serverUtxos::size);
			for (TransactionOutput out : serverUtxos) {

				if (out.getValue().isLessThan(targetUtxoValue.multiply(3))) {
					continue;
				}

				if (serverUtxos.size() == readyUtxoCount.get()) {
					break;
				}
				if (targetUtxoUpkeep <= readyUtxoCount.get()) {
					break;
				}

				if (out.getValue().isGreaterThan(targetUtxoValue.add(Coin.MICROCOIN))) {
					int maxOutput = DriverConstants.OUTPUT_COUNT_PER_TX; // To prevent excessive tx's
					Transaction tx = new Transaction(params);
					Coin currentVal = out.getValue().subtract(Transaction.DEFAULT_TX_FEE);
					List<ECKey> keys = new ArrayList<>();
					while (currentVal.isGreaterThan(targetUtxoValue) && maxOutput-- > 0) {
						ECKey key = new ECKey();
						keys.add(key);
						utxoWallet.importKey(key);
						tx.addOutput(targetUtxoValue, Address.fromKey(params, key, scriptType));

						currentVal = currentVal.subtract(targetUtxoValue);
					}
					tx.addInput(out);
					boolean sent;
					try {
						sent = driver.getWalletService(chain).completeAndSendTransaction(tx);
						if (!sent) {
							keys.forEach(utxoWallet::removeKey);
						}
					} catch (InsufficientMoneyException e) {
						log.error(e.getMessage());
						keys.forEach(utxoWallet::removeKey);
					}
				} else {
					continue;
				}

				readyUtxoCount.set(checkSuitableUtxos());
				log.debug("Wallet has UTXOs: {}", readyUtxoCount::get);

				if (targetUtxoUpkeep <= readyUtxoCount.get()) {
					return;
				} else {
					try {
						int counter;
						if (utxoWallet.getImportedKeys().size() < targetUtxoUpkeep) {
							counter = 15; // FIXME: Magic number
						} else {
							log.debug("Imported key count is larger than suitable UTXOs. Waiting for confirmations...");
							counter = 300; // FIXME: Magic number
						}
						while (driver.isOnline() && counter-- > 0) {
							TimeUnit.SECONDS.sleep(2);
						}
						splitUtxos();
					} catch (InterruptedException e) {
						log.error("Error during the park of UTXOProducer", e);
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void stop() {
		log.info("UTXO Producer is stopping...");
		executor.shutdown();
	}
}
