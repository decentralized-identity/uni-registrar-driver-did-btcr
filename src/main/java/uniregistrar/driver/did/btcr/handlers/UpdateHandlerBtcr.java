package uniregistrar.driver.did.btcr.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import foundation.identity.did.DIDDocument;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptPattern;

import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.ChainAndTxid;
import info.weboftrust.btctxlookup.bitcoinconnection.BTCDRPCBitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnectionException;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.funding.BtcrFund;
import uniregistrar.driver.did.btcr.funding.FundingException;
import uniregistrar.driver.did.btcr.state.SetCreateStateWaitConfirm;
import uniregistrar.driver.did.btcr.util.*;
import uniregistrar.request.UpdateRequest;
import uniregistrar.state.UpdateState;

/**
 * Class to handle update requests
 */
public class UpdateHandlerBtcr implements UpdateHandler {

	private static final Logger log = LogManager.getLogger(UpdateHandlerBtcr.class);
	private final DidBtcrDriver driver;
	private final DriverConfigs configs;

	public UpdateHandlerBtcr(DidBtcrDriver driver) {
		this.driver = driver;
		configs = driver.getConfigs();
	}

	@Override
	public UpdateState handle(final UpdateRequest request) throws RegistrationException {

		final String identifier = request.getIdentifier();
		final Chain chain = Chain.fromString(ParsingUtils.parseChain(request.getOptions()));
		log.debug("Update request will be processed on chain: {}", () -> chain);

		// Init a funding key
		ECKey secretKey;
		BtcrFund opFund;

		if (request.getSecret() == null) {
			throw new RegistrationException("Secret object is null!");
		}
		if (request.getSecret().get("privateKeyWiF") != null) {
			String fKeyStr = (String) request.getSecret().get("privateKeyWiF");
			secretKey = ECKeyUtils.convertWifKeyToECKey(fKeyStr, chain.toString());
		} else {
			throw new RegistrationException("No private key provided!");
		}

		Preconditions.checkNotNull(secretKey, "Cannot convert to ECKey");
		log.debug("Private Key is created from given secret.");

		ChainAndTxid chainAndTxid;
		log.debug("Looking up to chain and tx id resolution!");
		String onChainIdentifier = BTCRUtils.replaceHrpTxRef(identifier, chain.toString(), configs);

		Address toCheck = BitcoinUtils.getAddrStringFromKey(chain, secretKey, configs.getPrefScriptType());
		log.debug("Address to spend: {}", toCheck::toString);
		Transaction latestTX;

		try {
			Map<String, Long> unspents;
			if (configs.getBitcoinClientID() == BitcoinClientID.BITCOIND) {
				unspents = driver.getRpcClient(chain).findUnspents(toCheck.toString());
			} else if (configs.getBitcoinClientID() == BitcoinClientID.BTCD) {
				unspents = ((BTCDRPCBitcoinConnection) driver.getRpcClient(chain)).findUnspents(toCheck.toString(), 0,
						1, true);
			} else {
				throw new RegistrationException(
						configs.getBitcoinClientID().toString() + " does not support update and deactivation");
			}
			if (MapUtils.isEmpty(unspents)) {
				throw new RegistrationException("No unspents found for address: " + toCheck.toString());
			}
			latestTX = BitcoinUtils.fromHexToBitcoinJTX(unspents.keySet().iterator().next(), chain);

		} catch (ResolvingException | BitcoinConnectionException e) {
			throw new RegistrationException(e);
		}

		Preconditions.checkNotNull(latestTX, "Cannot locate the latest TX");
		Transaction updateTx = new Transaction(BitcoinUtils.chainToNetworkParameters(chain));

		TransactionOutput outputToSpend = null;

		Script op_return_script = null;
		for (TransactionOutput out : latestTX.getOutputs()) {
			if (!ScriptPattern.isOpReturn(out.getScriptPubKey())) {
				outputToSpend = out;
			} else {
				op_return_script = out.getScriptPubKey();
			}
		}

		String currentUri = null;

		if (op_return_script != null) {
			for (ScriptChunk chunk : op_return_script.getChunks()) {
				if (chunk.data != null) { // OP_RETURN consists of OP code and data part, select the data
					currentUri = new String(chunk.data, StandardCharsets.UTF_8);
				}
			}
		}

		int outputIndex = Preconditions.checkNotNull(outputToSpend).getIndex();
		boolean rotateKey = ParsingUtils.parseBoolean(request.getOptions().get("rotateKey"));

		try {
			opFund = driver.getFundingService(chain).getSelfFund(latestTX.getTxId().toString(), secretKey, outputIndex,
					rotateKey);
		} catch (FundingException e) {
			throw new RegistrationException(e);
		}

		URI didContinuationUri = null;
		if (!configs.isOverWriteDidDocs()) {
			didContinuationUri = configs.getDidDocContinuation().prepareDIDDocContinuation(null);
		} else if (currentUri != null) {
			try {
				didContinuationUri = new URI(currentUri);
			} catch (URISyntaxException e) {
				log.error(e.getMessage());
			}
		} else {
			didContinuationUri = configs.getDidDocContinuation().prepareDIDDocContinuation(null);
		}

		Preconditions.checkNotNull(didContinuationUri, "DID Cont. URI is null");

		updateTx.addOutput(Coin.ZERO,
				ScriptBuilder.createOpReturnScript(didContinuationUri.toString().getBytes(StandardCharsets.US_ASCII)));

		final Coin value = opFund.getAmount();
		if (value.isLessThan(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)) {
			throw new RegistrationException("Funding amount is lower than the fees!");
		}
		Address changeAddr = BitcoinUtils.getAddrStringFromKey(chain, opFund.getChangeKey(),
				toCheck.getOutputScriptType());

		updateTx.addOutput(value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE), changeAddr);

		updateTx.addSignedInput(opFund.getTransactionOutput(), opFund.getFundingKey(), Transaction.SigHash.ALL, false);

		Transaction sent;
		try {
			sent = driver.getWalletService(chain).sendCompletedTransaction(updateTx);
		} catch (TransactionException e) {
			throw new RegistrationException(e);
		}

		Preconditions.checkNotNull(sent, "Failed to send transaction");

		log.debug("Sent transaction! Transaction hash is {}", () -> sent.getTxId().toString());

		final DIDDocument doc = request.getDidDocument();

		DidBtcrJob job = new DidBtcrJob(chain, sent.getTxId().toString(), didContinuationUri, opFund.getFundingKey(),
				opFund.getChangeKey(), doc == null ? null : doc.getServices(),
				doc == null ? null : doc.getVerificationMethods(), doc == null ? null : doc.getAuthentications(),
				JobType.UPDATE, rotateKey, opFund.getFundingType());

		job.setIdentifier(identifier);

		driver.addNewJob(job);
		final String cJobId = job.getJobId();

		final UpdateState updateState = UpdateState.build();

		Map<String, Object> methodMetadata = new LinkedHashMap<>();
		methodMetadata.put("updateInitTime", BTCRUtils.getTimeStamp());
		methodMetadata.put("identifier", identifier);
		methodMetadata.put("chain", chain.toString());
		methodMetadata.put("transactionHash", sent.getTxId().toString());
		methodMetadata.put("balance", value
				.subtract(sent.getFee() == null ? Transaction.REFERENCE_DEFAULT_MIN_TX_FEE : sent.getFee()).getValue());
		methodMetadata.put("changeAddress", "" + changeAddr);
		methodMetadata.put("publicKeyHex", "" + opFund.getChangeKey().getPublicKeyAsHex());
		methodMetadata.put("didContinuationUri", "" + didContinuationUri);

		updateState.setJobId(job.getJobId());
		updateState.setMethodMetadata(methodMetadata);
		SetCreateStateWaitConfirm.setStateWaitConfirm(updateState, String
				.valueOf(BTCRUtils.estimateWaitingTime(configs, driver.getWalletService(chain).getBlockTimeSeconds())));
		updateState.setJobId(cJobId);

		driver.getBitcoinConfirmationTracker(chain).followTransaction(cJobId, sent.getTxId().toString());
		driver.getUpdateStates().put(job.getJobId(), updateState);

		return updateState;

	}
}
