package uniregistrar.driver.did.btcr.handlers;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptPattern;

import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.bitcoinconnection.BTCDRPCBitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnectionException;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.funding.BtcrFund;
import uniregistrar.driver.did.btcr.funding.FundingException;
import uniregistrar.driver.did.btcr.state.SetBtcrCreateStateFinished;
import uniregistrar.driver.did.btcr.state.SetCreateStateWaitConfirm;
import uniregistrar.driver.did.btcr.util.*;
import uniregistrar.request.DeactivateRequest;
import uniregistrar.state.DeactivateState;

public class DeactivationHandlerBtcr implements DeactivationHandler {

	private static final Logger log = LogManager.getLogger(DeactivationHandlerBtcr.class);
	private final DidBtcrDriver driver;
	private final DriverConfigs configs;

	public DeactivationHandlerBtcr(DidBtcrDriver driver) {
		this.driver = driver;
		configs = driver.getConfigs();
	}

	/**
	 * @param request Deactivation request
	 * @return State of the deactivation request
	 * @throws RegistrationException Indicator of any deactivation error
	 */
	@Override
	public DeactivateState handle(DeactivateRequest request) throws RegistrationException {

		final String did = request.getDid();
		if (StringUtils.isEmpty(did)) {
			throw new RegistrationException("Deactivation request has no did");
		}

		log.debug("Deactivation request for {}", () -> did);

		if (request.getSecret() == null || request.getSecret().get("privateKeyWiF") == null) {
			throw new RegistrationException("Secret object is null!");
		}

		final Chain chain = Chain.fromString(ParsingUtils.parseChain(request.getOptions()));
		log.debug("Deactivation request will be processed on chain: {}", () -> chain);

		// Get the next update key and its address: B2
		ECKey deactivateKey;

		if (request.getSecret().get("privateKeyWiF") != null) {
			String fKeyStr = (String) request.getSecret().get("privateKeyWiF");
			deactivateKey = ECKeyUtils.convertWifKeyToECKey(fKeyStr, chain.toString());
		} else {
			throw new RegistrationException("No private key provided!");
		}

		Preconditions.checkNotNull(deactivateKey, "Bad private key: Cannot convert it to ECKey");
		log.debug("Private Key is created from given secret.\nKey is: {}", deactivateKey::toString);

		Address toCheck = BitcoinUtils.getAddrStringFromKey(chain, deactivateKey, configs.getPrefScriptType());
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

		Transaction deactivateTx = new Transaction(BitcoinUtils.chainToNetworkParameters(chain));

		int outputIndex;

		Script op_return_script = null;
		TransactionOutput outputToSpend = null;

		for (TransactionOutput out : latestTX.getOutputs()) {
			if (ScriptPattern.isOpReturn(out.getScriptPubKey())) {
				op_return_script = out.getScriptPubKey();
			} else {
				outputToSpend = out;
			}
		}

		Preconditions.checkNotNull(outputToSpend, "Cannot locate output to spend!");
		Preconditions.checkArgument(outputToSpend.isAvailableForSpending(), "Output is already spent!");

		boolean rotateKey = ParsingUtils.parseBoolean(request.getOptions().get("rotateKey"));
		outputIndex = Preconditions.checkNotNull(outputToSpend).getIndex();

		BtcrFund opFund;
		try {
			opFund = driver.getFundingService(chain).getSelfFund(latestTX.getTxId().toString(), deactivateKey,
					outputIndex, rotateKey);
		} catch (FundingException e) {
			throw new RegistrationException(e);
		}

		String currentUri = null;

		if (op_return_script != null) {
			for (ScriptChunk chunk : op_return_script.getChunks()) {
				if (chunk.data != null) { // OP_RETURN consists of OP code and data part, select the data
					currentUri = new String(chunk.data, StandardCharsets.UTF_8);
				}
			}
		}

		Coin value = opFund.getAmount();
		if (value.isLessThan(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)) {
			throw new RegistrationException("Funding amount is lower than the fees!");
		}
		Address changeAddr = BitcoinUtils.getAddrStringFromKey(chain, opFund.getChangeKey(),
				toCheck.getOutputScriptType());

		deactivateTx.addOutput(value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE), changeAddr);

		deactivateTx.addSignedInput(opFund.getTransactionOutput(), opFund.getFundingKey(), Transaction.SigHash.ALL,
				false);
		Transaction sent;
		try {
			sent = driver.getWalletService(chain).sendCompletedTransaction(deactivateTx);
		} catch (TransactionException e) {
			throw new RegistrationException(e);
		}

		Preconditions.checkNotNull(sent, "Failed to send deactivation transaction");

		log.debug("Sent deactivation transaction! Transaction hash is {}", () -> sent.getTxId().toString());

		DidBtcrJob job = new DidBtcrJob(chain, sent.getTxId().toString(), null, opFund.getFundingKey(),
				opFund.getChangeKey(), null, null, null, JobType.DEACTIVATE, rotateKey, opFund.getFundingType());

		job.setDid(did);

		DeactivateState deactivateState = DeactivateState.build();

		final String cJobId = job.getJobId();

		Map<String, Object> didDocumentMetadata = new LinkedHashMap<>();
		didDocumentMetadata.put("deactivateInitTime", BTCRUtils.getTimeStamp());
		didDocumentMetadata.put("chain", chain.toString());
		didDocumentMetadata.put("transactionHash", sent.getTxId().toString());
		didDocumentMetadata.put("changeAddress", "" + changeAddr);
		didDocumentMetadata.put("balance", "" + value
				.subtract(sent.getFee() == null ? Transaction.REFERENCE_DEFAULT_MIN_TX_FEE : sent.getFee()).getValue());
//		didDocumentMetadata.put("old_did_continual_uri", "" + currentUri);
		didDocumentMetadata.put("operation", "deactivation");

		deactivateState.setJobId(job.getJobId());
		deactivateState.setDidDocumentMetadata(didDocumentMetadata);
		deactivateState.setJobId(cJobId);

		if (configs.isFollowDeactivate() || rotateKey) {
			SetCreateStateWaitConfirm.setStateWaitConfirm(deactivateState, String.valueOf(
					BTCRUtils.estimateWaitingTime(configs, driver.getWalletService(chain).getBlockTimeSeconds())));
			driver.getBitcoinConfirmationTracker(chain).followTransaction(cJobId, sent.getTxId().toString());
			driver.addNewJob(job);
		} else {
			SetBtcrCreateStateFinished.setStateFinished(deactivateState, did, null);
		}

		driver.getDeactivateStates().put(job.getJobId(), deactivateState);

		return deactivateState;
	}
}
