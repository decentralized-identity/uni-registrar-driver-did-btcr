package uniregistrar.driver.did.btcr.handlers;

import com.google.common.base.Preconditions;
import info.weboftrust.btctxlookup.Chain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.SendRequest;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.enums.FundingType;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.funding.BtcrFund;
import uniregistrar.driver.did.btcr.funding.FundingException;
import uniregistrar.driver.did.btcr.funding.FundingService;
import uniregistrar.driver.did.btcr.state.SetCreateStateWaitConfirm;
import uniregistrar.driver.did.btcr.util.*;
import uniregistrar.request.CreateRequest;
import uniregistrar.state.CreateState;
import uniregistrar.state.SetStateAction;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class to handle create requests
 */
public class CreateHandlerBtcr implements CreateHandler {

	private static final Logger log = LogManager.getLogger(CreateHandlerBtcr.class);

	private final DidBtcrDriver driver;
	private final DriverConfigs configs;

	/**
	 * Constructor for CreateHandler
	 * @param driver DidBtcrDriver
	 */
	public CreateHandlerBtcr(DidBtcrDriver driver) {
		this.driver = driver;
		this.configs = driver.getConfigs();
	}

	/**
	 * @param request Registration request
	 * @return State of the registration process
	 * @throws RegistrationException Registration exception
	 */
	@Override
	public CreateState handle(CreateRequest request, BtcrFund btcrFund) throws RegistrationException {
		// If no chain is provided default to TESTNET
		final Chain chain = Chain.fromString(ParsingUtils.parseChain(request.getOptions()));
		log.debug("Request will be processed on chain: {}", () -> chain);

		BtcrFund opFund = btcrFund;
		boolean rotateKey = ParsingUtils.parseBoolean(request.getOptions().get("rotateKey"));
		FundingService fundingService = driver.getFundingService(chain);

		if (opFund == null) {

			// Check if a funding key is provided
			// Initially expect funding tx to be provided.
			if (request.getSecret() != null && request.getSecret().get("privateKeyWiF") != null
					&& request.getSecret().get("fundingTX") != null && request.getSecret().get("outputIndex") != null) {
				String fKeyStr = (String) request.getSecret().get("privateKeyWiF");
				String fTx = (String) request.getSecret().get("fundingTX");
				log.debug("Operation is self funded. Funding key: {} \nFunding TX: {}", fKeyStr, fTx);
				// Init a funding key
				ECKey fundingKey;
				fundingKey = ECKeyUtils.convertWifKeyToECKey(fKeyStr, chain.toString());

				if (rotateKey) {
					try {
						opFund = fundingService.getSelfFund(fTx, fundingKey,
															(Integer) request.getSecret().get("outputIndex"), true);
					} catch (FundingException e) {
						throw new RegistrationException(e);
					}
				}
				else {
					try {
						opFund = fundingService.getSelfFund(fTx, fundingKey,
															(Integer) request.getSecret().get("outputIndex"), false);
					} catch (FundingException e) {
						throw new RegistrationException(e);
					}
				}
			}
			else if (request.getSecret() != null && request.getSecret().get("fundingTicket") != null) {
				// If user funded the given address, it should be available
				try {
					String fTicket = (String) request.getSecret().get("fundingTicket");
					log.debug("A funding ticket ({}) is given.", () -> fTicket);
					opFund = fundingService.getExpectedFund(fTicket, true);
				} catch (FundingException e) {
					throw new RegistrationException(e.getMessage());
				}
			}
			else {
				if (fundingService.canServerFund()) {
					log.debug("Trying to allocate a server-paid Fund");
					try {
						opFund = fundingService.getServerSideFund();
					} catch (FundingException e) {
						throw new RegistrationException(e);
					}
					if (opFund == null) {
						log.debug("!!Ups!! Either server processing insane amount of requests or we found a bug!");
						String message = fundingService.askForFundingString();
						throw new RegistrationException(message);
					}
				}
				else {
					log.debug("Wallet response no to funding request...");
					BtcrFund fund = fundingService.prepareFund();
					String message = fundingService.askForFundingString(fund);
					final DidBtcrJob job = new DidBtcrJob(fund.getUuid(), chain, null, null, fund.getFundingKey(), fund.getChangeKey(),
														  request.getDidDocument() == null ? null : request.getDidDocument().getServices(),
														  request.getDidDocument() == null ? null : request.getDidDocument().getVerificationMethods(),
														  request.getDidDocument() == null ? null : request.getDidDocument()
																										   .getAuthenticationVerificationMethods(),
														  JobType.CREATE, rotateKey, fund.getFundingType());

					driver.addFundingRequiredJob(job);
					CreateState state = CreateState.build();
					state.setJobId(job.getJobId());
					SetStateAction.setStateAction(state, message);
					return state;
				}
			}
		}

		Optional<DidBtcrJob> fundingRequiredJob = driver.getAndRemoveFundingRequiredJob(opFund.getUuid());
		fundingRequiredJob.ifPresent(job -> log.debug("Funding required job '{}' is found", job.getJobId()));

		URI didContinuationUri = null;
		Transaction tx = new Transaction(BitcoinUtils.chainToNetworkParameters(chain));

		if (request.getDidDocument() != null && ((request.getDidDocument().getServices() != null
				&& !request.getDidDocument().getServices().isEmpty())
				|| (request.getDidDocument().getVerificationMethods() != null
				&& !request.getDidDocument().getVerificationMethods().isEmpty())
				|| (request.getDidDocument().getAuthenticationVerificationMethods() != null
				&& !request.getDidDocument().getAuthenticationVerificationMethods().isEmpty()))) {
			log.debug("Request is customized, preparing a did-continuation document...");
			didContinuationUri = configs.getDidDocContinuation().prepareDIDDocContinuation(null);
			log.debug("didContinuationUri: {}", didContinuationUri::toString);
		}
		else if (fundingRequiredJob.isPresent()) {
			DidBtcrJob j = fundingRequiredJob.get();
			if ((j.getAddServices() != null
					&& !j.getAddServices().isEmpty())
					|| (j.getAddVerificationMethods() != null
					&& !j.getAddVerificationMethods().isEmpty()
					|| (j.getAddAuthentications() != null
					&& !j.getAddAuthentications().isEmpty()))) {
				log.debug("Request is customized, preparing a did-continuation document...");
				didContinuationUri = configs.getDidDocContinuation().prepareDIDDocContinuation(null);
				log.debug("didContinuationUri: {}", didContinuationUri::toString);
			}
		}
		else {
			log.debug("No extra specification given, DDO won't be created.");
			didContinuationUri = null;
		}

		log.debug("Preparing continuation DID Document: {}",
				  didContinuationUri != null ? didContinuationUri.toString() : "empty");

		if (didContinuationUri != null) {
			log.debug("Creating a OP_RETURN script for the didContinuationUri...");
			tx.addOutput(Coin.ZERO, ScriptBuilder
					.createOpReturnScript(didContinuationUri.toString().getBytes(StandardCharsets.US_ASCII)));
		}

		final Address changeAddress = BitcoinUtils.getAddrStringFromKey(chain, opFund.getChangeKey(),
																		configs.getPrefScriptType());
		opFund.setChangeAddress(changeAddress);
		final Script outputScript = ScriptBuilder.createOutputScript(changeAddress);
		tx.addOutput(opFund.getAmount().subtract(DriverConstants.DEFAULT_TX_FEE), outputScript);

		log.debug("Script output address is: {}", () -> changeAddress);

		Transaction sent;
		if (opFund.getFundingType() == FundingType.USER) {
			tx.addSignedInput(opFund.getTransactionOutput(), opFund.getFundingKey(), Transaction.SigHash.ALL, false);
			try {
				sent = driver.getWalletService(chain).sendCompletedTransaction(tx);
			} catch (TransactionException e) {
				throw new RegistrationException(e.getMessage(), e);
			}
		}
		else {
			tx.addInput(opFund.getTransactionOutput());
			SendRequest sr;
			try {
				sr = driver.getWalletService(chain).signUtxoWalletTxThenSend(tx);
			} catch (TransactionException e) {
				throw new RegistrationException(e);
			}
			sent = sr.tx;
		}

		Preconditions.checkNotNull(sent, "Failed to btcrFund operation!");

		final String txID = sent.getTxId().toString();

		log.debug("Sent transaction! Transaction hash is: {}", txID);
		if (log.isDebugEnabled()) {
			for (TransactionInput input : sent.getInputs()) {
				log.debug("Transaction input: {} {}", input.getValue(), input);
			}
		}

		if (log.isDebugEnabled()) {
			for (TransactionOutput out : sent.getOutputs()) {
				log.debug("Transaction output: {} {}", out.getValue(), out);
			}
		}

		ECKey changeKey = opFund.getChangeKey();
		DidBtcrJob job;
		// CREATE STATE WAIT: JOBID
		if (fundingRequiredJob.isPresent()) {
			DidBtcrJob j = fundingRequiredJob.get();
			job = new DidBtcrJob(j.getJobId(), chain, txID, didContinuationUri, opFund.getFundingKey(), changeKey,
								 j.getAddServices(),
								 j.getAddVerificationMethods(),
								 j.getAddAuthentications(),
								 JobType.CREATE, rotateKey, opFund.getFundingType());
		}
		else {
			job = new DidBtcrJob(chain, txID, didContinuationUri, opFund.getFundingKey(), changeKey,
								 request.getDidDocument() == null ? null : request.getDidDocument().getServices(),
								 request.getDidDocument() == null ? null : request.getDidDocument().getVerificationMethods(),
								 request.getDidDocument() == null ? null : request.getDidDocument().getAuthenticationVerificationMethods(),
								 JobType.CREATE, rotateKey, opFund.getFundingType());
		}

		final String cJobId = job.getJobId();

		// follow transaction state
		driver.getBitcoinConfirmationTracker(chain).followTransaction(cJobId, sent.getTxId().toString());

		// CREATE STATE WAIT: METHOD METADATA

		String publicKeyHex;
		if (opFund.getChangeKey() == null) {
			throw new RegistrationException("Something went very wrong, changeKey key is not available");
		}

		publicKeyHex = changeKey.getPublicKeyAsHex();

		final Map<String, Object> didDocumentMetadata = new LinkedHashMap<>();
		didDocumentMetadata.put("createInitTime", BTCRUtils.getTimeStamp());
		didDocumentMetadata.put("chain", chain.toString());
		didDocumentMetadata.put("transactionHash", txID);
		didDocumentMetadata.put("balance", opFund.getAmount()
											.subtract(sent.getFee() == null ? Transaction.REFERENCE_DEFAULT_MIN_TX_FEE : sent.getFee())
											.getValue());
		didDocumentMetadata.put("changeAddress", "" + changeAddress);
		didDocumentMetadata.put("publicKeyHex", "" + publicKeyHex);
		didDocumentMetadata.put("didContinuationUri", "" + didContinuationUri);

		// done

		final CreateState state = CreateState.build();

		SetCreateStateWaitConfirm.setStateWaitConfirm(state, String
				.valueOf(BTCRUtils.estimateWaitingTime(configs, driver.getWalletService(chain).getBlockTimeSeconds())));
		state.setJobId(job.getJobId());
		state.setDidDocumentMetadata(didDocumentMetadata);

		state.setJobId(job.getJobId());
		driver.addNewJob(job);
		driver.getCreateStates().put(cJobId, state);

		if (opFund.getFundingType() == FundingType.SERVER) {
			driver.removeUtxoKey(opFund.getFundingKey(), chain);
		}

		log.debug("First phase of the registration is finished. Create state is: {}", () -> state);

		return state;
	}
}
