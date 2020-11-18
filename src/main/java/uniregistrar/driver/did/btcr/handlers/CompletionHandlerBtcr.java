package uniregistrar.driver.did.btcr.handlers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import foundation.identity.did.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;

import com.nimbusds.jose.jwk.JWK;

import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.ChainAndLocationData;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.diddoccontinuation.DIDDocContinuation;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.service.ExecutorProvider;
import uniregistrar.driver.did.btcr.state.SetBtcrRegisterStateFinished;
import uniregistrar.driver.did.btcr.util.BTCRUtils;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;
import uniregistrar.driver.did.btcr.util.ECKeyUtils;
import uniregistrar.driver.did.btcr.util.NetworkUtils;
import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.SetRegisterStateFinished;
import uniregistrar.state.UpdateState;

/**
 * This class finalizes the requests when created jobs are ready for completion
 */
public class CompletionHandlerBtcr implements CompletionHandler {

	private static final Logger log = LogManager.getLogger(CompletionHandlerBtcr.class);
	private final DidBtcrDriver driver;
	private final BlockingQueue<DidBtcrJob> cJobs;
	private final ExecutorService executors;
	private final DriverConfigs configs;

	/**
	 * @param driver Creator of this class
	 */
	public CompletionHandlerBtcr(DidBtcrDriver driver) {
		this.driver = driver;
		configs = driver.getConfigs();
		cJobs = driver.getCompletionQueue();
		executors = ExecutorProvider.getFixedThreadPool(getClass().getSimpleName(), 5);
	}

	/**
	 * Starts with sending a thread to wait for completion queue
	 */
	@Override
	public void start() {
		log.info("Starting the completion handler...");
		CompletableFuture.runAsync(this::dispatch);
	}

	private void dispatch() {
		while (driver.isOnline()) {
			DidBtcrJob job;
			try {
				// To catchup in case of shutdown
				job = cJobs.poll(2, TimeUnit.SECONDS);
				if (job == null) {
					continue;
				}
				if (job.getJobType() == JobType.REGISTER) {
					CompletableFuture.runAsync(() -> {
						try {
							completeRegistration(job);
						} catch (IOException | RegistrationException e) {
							log.error("Cannot complete the registrations! {}", e::getMessage);
							driver.jobFailed(job.getJobId(), e.getMessage());
						}
					}, executors);
				} else if (job.getJobType() == JobType.UPDATE) {
					CompletableFuture.runAsync(() -> {
						try {
							completeUpdate(job);
						} catch (RegistrationException | IOException e) {
							log.error("Cannot complete the update! {}", e::getMessage);
							driver.jobFailed(job.getJobId(), e.getMessage());
						}
					}, executors);
				} else if (job.getJobType() == JobType.DEACTIVATE) {
					CompletableFuture.runAsync(() -> {
						try {
							completeDeactivate(job);
						} catch (RegistrationException | IOException e) {
							log.error("Cannot complete the deactivation! {}", e::getMessage);
							driver.jobFailed(job.getJobId(), e.getMessage());
						}
					}, executors);
				} else {
					throw new IllegalArgumentException("Unknown JobType");
				}
			} catch (InterruptedException e) {
				log.debug("Completion worker is interrupted");
				log.debug("InterruptedException: {}", () -> e);
			}
		}
	}

	@Override
	public void stop() {
		log.debug("Stopping the completion worker...");
		if (executors != null && !executors.isTerminated()) {
			executors.shutdown();
		}
	}

	public void completeUpdate(DidBtcrJob job) throws IOException, RegistrationException {
		Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());
		log.debug("Received an update-completion job: {}", job::getJobId);

		final Chain chain = job.getChain();
		final String txHash = job.getTransactionHash();
		final URI didContinuationUri = job.getDidContinuationUri();
		final ECKey privateKey = job.getChangeKey();
		final List<Service> addServices = job.getAddServices();
		final List<VerificationMethod> verificationMethods = job.getAddVerificationMethods();
		final List<Authentication> addAuthentications = job.getAddAuthentications();
		final NetworkParameters params = BitcoinUtils.chainToNetworkParameters(chain);

		final ChainAndLocationData chainAndLocationData = NetworkUtils.getChainAndLocationData(chain, txHash,
				driver.getRpcClient(chain));
		final String did = job.getIdentifier();

		if (didContinuationUri != null) {
			log.debug("Storing the DID Continuation Document with URI: {}", () -> didContinuationUri);

			final DIDDocument didContinuationDocument = DIDDocument.builder().id(URI.create(did))
					.verificationMethods(verificationMethods).authentications(addAuthentications).services(addServices)
					.build();

			log.debug("DIDDocument created for DID: {}", didContinuationDocument::getId);
			final DIDDocContinuation didContinuation = configs.getDidDocContinuation();

			try {
				didContinuation.storeDIDDocContinuation(didContinuationUri, didContinuationDocument);
			} catch (IOException e) {
				throw new RegistrationException(e);
			}
		} else {
			throw new RegistrationException("Update has no didContinuationUri!");
		}

		Map<String, Object> secret = new LinkedHashMap<>();

		if (job.isRotateKey()) {
			final String publicKeyHex = privateKey.getPublicKeyAsHex();
			final String privateKeyHex = privateKey.getPrivateKeyAsHex();
			final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
			final JWK jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey);
			final String publicKeyDIDURL = BTCRUtils.identifierToPublicKeyDIDURL(did);

			final List<Map<String, Object>> jsonKeys = new ArrayList<>();

			final Map<String, Object> jsonKey = new LinkedHashMap<>();
			jsonKey.put("publicKeyHex", publicKeyHex);
			jsonKey.put("privateKeyHex", privateKeyHex);
			jsonKey.put("privateKeyWif", privateKeyWif);
			jsonKey.put("privateKeyJwk", jsonWebKey.toJSONObject());
			jsonKey.put("publicKeyDIDURL", publicKeyDIDURL);
			jsonKeys.add(jsonKey);

			secret.put("updateKeys", jsonKeys);
		}

		// Update is finished. Prepare the result.
		final Map<String, Object> methodMetadata = new LinkedHashMap<>();
		methodMetadata.put("updateCompletionTime", BTCRUtils.getTimeStamp());
		methodMetadata.put("chain", chain);
		methodMetadata.put("transactionHash", txHash);
		methodMetadata.put("blockHeight", chainAndLocationData.getLocationData().getBlockHeight());
		methodMetadata.put("transactionPosition", chainAndLocationData.getLocationData().getTransactionPosition());
		methodMetadata.put("didContinuationUri", "" + didContinuationUri);
		methodMetadata.put("operation", "update");

		final UpdateState state = UpdateState.build();

		SetBtcrRegisterStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Update state for the job {} is completed", job::getJobId);
		log.debug("Update State: {}", state::toString);
		driver.jobCompleted(job.getJobId());
		driver.getUpdateStates().put(job.getJobId(), state);

	}

	public void completeRegistration(DidBtcrJob job) throws RegistrationException, IOException {
		Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());
		log.debug("Received a register-completion job: {}", job::getJobId);

		final Chain chain = job.getChain();
		final String txHash = job.getTransactionHash();
		final URI didContinuationUri = job.getDidContinuationUri();
		final ECKey privateKey = job.getPrivateKey();
		final List<Service> addServices = job.getAddServices();
		final List<VerificationMethod> verificationMethods = job.getAddVerificationMethods();
		final List<Authentication> addAuthentications = job.getAddAuthentications();
		final NetworkParameters params = BitcoinUtils.chainToNetworkParameters(chain);

		final ChainAndLocationData chainAndLocationData = NetworkUtils.getChainAndLocationData(chain, txHash,
				driver.getRpcClient(chain));
		final String txref = ChainAndLocationData.txrefEncode(chainAndLocationData);
		final String did = configs.getMethodPrefix() + BTCRUtils.stripTxref(txref);

		if (didContinuationUri != null) {
			log.debug("Storing the DID Continuation Document with URI: {}", didContinuationUri::toString);

			final DIDDocument didContinuationDocument = DIDDocument.builder().id(URI.create(did))
					.verificationMethods(verificationMethods).authentications(addAuthentications).services(addServices)
					.build();

			log.debug("Created DIDDocument for DID: {}", didContinuationDocument::getId);
			final DIDDocContinuation didContinuation = configs.getDidDocContinuation();

			try {
				didContinuation.storeDIDDocContinuation(didContinuationUri, didContinuationDocument);
			} catch (IOException e) {
				throw new RegistrationException(e.getMessage());
			}
		}

		final String publicKeyHex = privateKey.getPublicKeyAsHex();
		final String privateKeyHex = privateKey.getPrivateKeyAsHex();
		final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
		final JWK jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey);
		final String publicKeyDIDURL = BTCRUtils.identifierToPublicKeyDIDURL(did);

		final ECKey changeKey = job.getChangeKey();

		final String publicKeyHex2 = changeKey.getPublicKeyAsHex();
		final String privateKeyHex2 = changeKey.getPrivateKeyAsHex();
		final String privateKeyWif2 = changeKey.getPrivateKeyAsWiF(params);
		final JWK jsonWebKey2 = ECKeyUtils.privateKeyToJWK(changeKey);

		final List<Map<String, Object>> jsonKeys = new ArrayList<>();

		final Map<String, Object> jsonKey = new LinkedHashMap<>();
		jsonKey.put("publicKeyHex", publicKeyHex);
		jsonKey.put("privateKeyHex", privateKeyHex);
		jsonKey.put("privateKeyWif", privateKeyWif);
		jsonKey.put("privateKeyJwk", jsonWebKey.toJSONObject());
		jsonKey.put("publicKeyDIDURL", publicKeyDIDURL);
		jsonKeys.add(jsonKey);

		final Map<String, Object> secret = new LinkedHashMap<>();
		secret.put("register_keys", jsonKeys);

		final List<Map<String, Object>> jsonKeys2 = new ArrayList<>();

		final Map<String, Object> jsonKey2 = new LinkedHashMap<>();
		jsonKey2.put("publicKeyHex", publicKeyHex2);
		jsonKey2.put("privateKeyHex", privateKeyHex2);
		jsonKey2.put("privateKeyWif", privateKeyWif2);
		jsonKey2.put("privateKeyJwk", jsonWebKey2.toJSONObject());
		jsonKeys2.add(jsonKey2);

		secret.put("updateKeys", jsonKeys2);

		// Registration is finished. Prepare the result.
		final Map<String, Object> methodMetadata = new LinkedHashMap<>();
		methodMetadata.put("registerCompletionTime", BTCRUtils.getTimeStamp());
		methodMetadata.put("chain", chain);
		methodMetadata.put("transactionHash", txHash);
		methodMetadata.put("blockHeight", chainAndLocationData.getLocationData().getBlockHeight());
		methodMetadata.put("transactionPosition", chainAndLocationData.getLocationData().getTransactionPosition());
		methodMetadata.put("txoIndex", chainAndLocationData.getLocationData().getTxoIndex());
		methodMetadata.put("didContinuationUri", "" + didContinuationUri);
		methodMetadata.put("operation", "register");

		final RegisterState state = RegisterState.build();

		SetRegisterStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Registration state for the job {} is completed", job::getJobId);
		log.debug("Registration is completed: {}", () -> job);
		log.debug("Registration State: {}", state::toString);

//		if (job.getFundingType() == FundingType.SERVER) {
//			driver.removeUtxoKey(job.getPrivateKey(), chain);
//		}
		driver.jobCompleted(job.getJobId());
		driver.getRegisterStates().put(job.getJobId(), state);

	}

	public void completeDeactivate(DidBtcrJob job) throws IOException, RegistrationException {
		Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());
		log.debug("Received an deactivation-completion job: {}", job::getJobId);

		final Chain chain = job.getChain();
		final String txHash = job.getTransactionHash();
		final URI didContinuationUri = job.getDidContinuationUri();
		final ECKey privateKey = job.getPrivateKey();
		final NetworkParameters params = BitcoinUtils.chainToNetworkParameters(chain);

		final ChainAndLocationData chainAndLocationData = NetworkUtils.getChainAndLocationData(chain, txHash,
				driver.getRpcClient(chain));
		final String did = job.getIdentifier();

		if (didContinuationUri != null) {
			log.debug("Storing the DID Continuation Document with URI: {}", () -> didContinuationUri);
			throw new RegistrationException("Deactivation has didContinuationUri: {}" + didContinuationUri);
		}

		Map<String, Object> secret = new LinkedHashMap<>();

		if (job.isRotateKey()) {
			final String publicKeyHex = privateKey.getPublicKeyAsHex();
			final String privateKeyHex = privateKey.getPrivateKeyAsHex();
			final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
			final JWK jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey);
			final String publicKeyDIDURL = BTCRUtils.identifierToPublicKeyDIDURL(did);

			final List<Map<String, Object>> jsonKeys = new ArrayList<>();

			final Map<String, Object> jsonKey = new LinkedHashMap<>();
			jsonKey.put("publicKeyHex", publicKeyHex);
			jsonKey.put("privateKeyHex", privateKeyHex);
			jsonKey.put("privateKeyWif", privateKeyWif);
			jsonKey.put("privateKeyJwk", jsonWebKey.toJSONObject());
			jsonKey.put("publicKeyDIDURL", publicKeyDIDURL);
			jsonKeys.add(jsonKey);

			secret.put("spendingKeys", jsonKeys);
		}

		// Update is finished. Prepare the result.
		final Map<String, Object> methodMetadata = new LinkedHashMap<>();
		methodMetadata.put("deactivateCompletionTime", BTCRUtils.getTimeStamp());
		methodMetadata.put("chain", chain.toString());
		methodMetadata.put("transactionHash", txHash);
		methodMetadata.put("blockHeight", chainAndLocationData.getLocationData().getBlockHeight());
		methodMetadata.put("transactionPosition", chainAndLocationData.getLocationData().getTransactionPosition());
		methodMetadata.put("operation", "deactivation");

		final DeactivateState state = DeactivateState.build();

		SetBtcrRegisterStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Deactivation state for the job {} is completed", job::getJobId);
		log.debug("Deactivation State: {}", state::toString);
		driver.jobCompleted(job.getJobId());
		driver.getDeactivateStates().put(job.getJobId(), state);
	}

}
