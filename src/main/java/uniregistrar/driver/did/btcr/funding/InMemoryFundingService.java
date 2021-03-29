package uniregistrar.driver.did.btcr.funding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.wallet.Wallet;

import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnectionException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.enums.FundingType;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;
import uniregistrar.driver.did.btcr.util.ECKeyUtils;

/**
 * Manages the transaction funding
 */
public class InMemoryFundingService implements FundingService {

	private static final Logger log = LogManager.getLogger(InMemoryFundingService.class);
	// Keeps keys for marking incoming Transactions as a funding
	private final Map<String, BtcrFund> expectedFunds;
	private final DidBtcrDriver driver;
	private final Chain chain;
	private final DriverConfigs driverConfigs;
	private final NetworkParameters params;

	/**
	 *
	 * Expected {@link BtcrFund} entities are kept in {@link PassiveExpiringMap}
	 * Server side {@link BtcrFund} obtained from UTXO-Wallets
	 * 
	 * @implNote This implementation has no persistence
	 * 
	 * @param driver {@link DidBtcrDriver}
	 * @param chain  {@link Chain}
	 */
	public InMemoryFundingService(DidBtcrDriver driver, Chain chain) {
		this.driver = driver;
		this.driverConfigs = driver.getConfigs();
		this.chain = chain;
		this.params = BitcoinUtils.chainToNetworkParameters(chain);
		this.expectedFunds = new PassiveExpiringMap<>(DriverConstants.DEFAULT_FUNDING_WAIT_TIME, TimeUnit.HOURS,
				new ConcurrentHashMap<>());
	}

	@Override
	public BtcrFund getServerSideFund() throws FundingException {
		log.debug("Server-side funding request received...");
		Wallet wallet = driver.getUtxoWallet(chain);
		Context.propagate(driver.getContext(chain));

		if (!canServerFund()) {
			throw new FundingException("Wallet has no funds, cannot fund any operation!");
		}

		final List<TransactionOutput> utxos = wallet.calculateAllSpendCandidates(true, true);
		log.debug("There are {} UTXOs in the server wallet!", utxos::size);

		if (utxos.size() == 0) {
			throw new FundingException("No UTXO to fund!");
		}
		TransactionOutput toSpend = utxos.get(0);

//		utxos.sort(Comparator.comparing(TransactionOutput::getValue));
//		log.debug("Min amount of UTXO in the wallet is {}, max. amount is {}", () -> utxos.get(0),
//				() -> utxos.get(utxos.size() - 1));
//		TransactionOutput toSpend;
//		Coin targetValue = Coin.valueOf(driverConfigs.getTargetFundAmount() - 1);
//
//		Optional<TransactionOutput> utxoToUse = (utxos.stream()).parallel()
//				.filter(o -> o.getValue().isGreaterThan(targetValue.subtract(Coin.SATOSHI))
//						&& targetValue.multiply(20).isGreaterThan(o.getValue()))
//				.findAny();

//		if (!utxoToUse.isPresent()) {
//			log.debug("No suitable UTXO is found!");
//			return null;
//		}

//		toSpend = utxoToUse.get();
		ECKey fundingKey = wallet.findKeyFromPubKeyHash(toSpend.getScriptPubKey().getPubKeyHash(),
				ScriptPattern.isP2PKH(toSpend.getScriptPubKey()) ? Script.ScriptType.P2PKH : Script.ScriptType.P2WPKH);
		log.debug("Suitable UTXO is found {}", toSpend::toString);

		TransactionOutPoint outPoint = new TransactionOutPoint(params, toSpend);

		return BtcrFund.Builder.newFund().withAmount(toSpend.getValue()).withFundingKey(fundingKey)
				.withTxOutPoint(outPoint).withTransactionOutput(toSpend).withOutIndex(toSpend.getIndex())
				.withFundingType(FundingType.SERVER).withChangeKey(new ECKey()).build();
	}

	@Override
	public BtcrFund getSelfFund(String txid, ECKey privKey, @Nullable Integer outIndex, boolean rotateKey)
			throws FundingException {

		Transaction fundingTX;

		try {
			String txhex = driver.getRpcClient(chain).getRawTransactionHex(txid);
			fundingTX = BitcoinUtils.fromHexToBitcoinJTX(txhex, chain);
		} catch (BitcoinConnectionException e) {
			throw new FundingException(e);
		}

		Preconditions.checkNotNull(fundingTX, "Funding TX is null!");
		int oin = outIndex == null ? 0 : outIndex;
		TransactionOutput output = fundingTX.getOutput(oin);
		Coin value = output.getValue();

		ECKey changeKey = rotateKey ? ECKeyUtils.getFreshKey() : privKey;

		BtcrFund toRet = BtcrFund.Builder.newFund().withFundingKey(privKey).withFundingType(FundingType.USER)
				.withTxID(txid).withTransactionOutput(output).withAmount(value).withOutIndex(oin)
				.withChangeKey(changeKey).build();
		log.debug("Created client fund: {}", () -> toRet);

		return toRet;
	}

	public BtcrFund getExpectedFund(String fundingTicket, boolean rotateKey) throws FundingException {
		BtcrFund expectedFund = expectedFunds.get(fundingTicket);
		if (expectedFund == null) {
			log.debug("Failed to find/load the fund for given funding ticket: {} ", fundingTicket);
			throw new FundingException("No such a fund is found!");
		} else {
			log.debug("Fund found. Removing it from the expected-funds!");
			expectedFunds.remove(fundingTicket);
		}

		Map<String, Long> unspents;
		TransactionOutPoint outPoint;
		try {
			unspents = driver.getRpcClient(chain).findUnspents(expectedFund.getFundAddress().toString());
			Map.Entry<String, Long> entry = unspents.entrySet().iterator().next();
			Transaction wtx = BitcoinUtils.fromHexToBitcoinJTX(entry.getKey(), chain);
			outPoint = new TransactionOutPoint(params, entry.getValue(), wtx);
		} catch (BitcoinConnectionException e) {
			throw new FundingException(e);
		}

		if (outPoint.getConnectedOutput() == null) {
			throw new FundingException("Cannot locate the user fund!");
		}

		log.debug("User fund is preparing with: {}", outPoint::toString);

		expectedFund.setTxID(outPoint.getHash().toString());
		expectedFund.setAmount(outPoint.getConnectedOutput().getValue());
		expectedFund.setOutIndex(outPoint.getConnectedOutput().getIndex());
		expectedFund.setTransactionOutput(outPoint.getConnectedOutput());
		expectedFund.setFundingType(FundingType.USER);
		expectedFund.setChangeKey(rotateKey ? ECKeyUtils.getFreshKey() : expectedFund.getFundingKey());

		return expectedFund;
	}

	@Override
	public boolean canServerFund() {
		Context.propagate(driver.getContext(chain));
		Wallet wallet = driver.getUtxoWallet(chain);
		if(wallet == null){
			return false;
		}
		return wallet.calculateAllSpendCandidates(true, true).size() > 0;
	}

	public Map<String, String> askForFunding() {
		Map<String, String> funding = new LinkedHashMap<>();
		String ticket = UUID.randomUUID().toString();
		funding.put("ticket", ticket);
		ECKey expectedFundKey = ECKeyUtils.getFreshKey();
		Address address = Address.fromKey(params, expectedFundKey, driverConfigs.getPrefScriptType());
		funding.put("address", address.toString());
		BtcrFund fund = BtcrFund.Builder.newFund().withFundingType(FundingType.USER).withFundingKey(expectedFundKey)
				.withUuid(ticket).withFundingAddress(address).build();

		expectedFunds.put(ticket, fund);
		log.debug("New expected fund is added with ticket {}, and fund address {}", () -> ticket, fund::getFundAddress);
		log.debug("Expected funds size: {}", expectedFunds::size);

		return funding;
	}

	public String askForFundingString() {
		String ticket = UUID.randomUUID().toString();
		ECKey expectedFundKey = ECKeyUtils.getFreshKey();
		Address address = Address.fromKey(params, expectedFundKey, driverConfigs.getPrefScriptType());
		BtcrFund fund = BtcrFund.Builder.newFund().withFundingType(FundingType.USER).withFundingKey(expectedFundKey)
				.withUuid(ticket).withFundingAddress(address).build();

		expectedFunds.put(ticket, fund);
		log.debug("New expected fund is added with ticket {}, and fund address {}", () -> ticket, fund::getFundAddress);
		log.debug("Expected funds size: {}", expectedFunds::size);

		return "Please fund the address " + address.toString() + " and re-submit the operation with the property \"fundingTicket\" set to value \"" + ticket + "\" in the \"secret\" request object.";
	}
}
