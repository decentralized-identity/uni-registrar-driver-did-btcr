package uniregistrar.driver.did.btcr.handlers;

import foundation.identity.did.DIDDocument;
import foundation.identity.did.Service;
import foundation.identity.did.VerificationMethod;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.ChainAndLocationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.diddoccontinuation.DIDDocContinuation;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.service.ExecutorProvider;
import uniregistrar.driver.did.btcr.state.SetBtcrCreateStateFinished;
import uniregistrar.driver.did.btcr.util.BTCRUtils;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;
import uniregistrar.driver.did.btcr.util.ECKeyUtils;
import uniregistrar.driver.did.btcr.util.NetworkUtils;
import uniregistrar.state.CreateState;
import uniregistrar.state.DeactivateState;
import uniregistrar.state.SetCreateStateFinished;
import uniregistrar.state.UpdateState;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
				if (job.getJobType() == JobType.CREATE) {
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
				Thread.currentThread().interrupt();
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
		final List<VerificationMethod> addAuthentications = job.getAddAuthentications();
		final NetworkParameters params = BitcoinUtils.chainToNetworkParameters(chain);

		final ChainAndLocationData chainAndLocationData = NetworkUtils.getChainAndLocationData(chain, txHash,
				driver.getRpcClient(chain));
		final String did = job.getIdentifier();

		if (didContinuationUri != null) {
			log.debug("Storing the DID Continuation Document with URI: {}", () -> didContinuationUri);

			final DIDDocument didContinuationDocument = DIDDocument.builder().id(URI.create(did))
					.verificationMethods(verificationMethods).authenticationVerificationMethods(addAuthentications).services(addServices)
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
			final Map<String, Object> jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey).toMap();
			// Remove null values
			jsonWebKey.values().removeAll(Collections.singleton(null));
			final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
			final String privateKeyBase58 = Base58.encode(privateKey.getPrivKeyBytes());

			final Map<String, Object> jsonKey = new LinkedHashMap<>();
			jsonKey.put("type",ECKeyUtils.LD_KEY_TYPE);
			jsonKey.put("privateKeyJwk", jsonWebKey);
			jsonKey.put("privateKeyBase58", privateKeyBase58);
			jsonKey.put("privateKeyWif", privateKeyWif);
			jsonKey.put("purpose", new String[]{"update", "deactivate"});

			secret.put("verificationMethod", jsonKey);
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

		SetBtcrCreateStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Update state for the job {} is completed", job::getJobId);
		log.debug("Update State: {}", state::toString);
		driver.jobCompleted(job.getJobId());
		driver.getUpdateStates().put(job.getJobId(), state);

	}

	public void completeRegistration(DidBtcrJob job) throws RegistrationException, IOException {
		Thread.currentThread().setContextClassLoader(driver.getClass().getClassLoader());
		log.debug("Received a create-completion job: {}", job::getJobId);

		final Chain chain = job.getChain();
		final String txHash = job.getTransactionHash();
		final URI didContinuationUri = job.getDidContinuationUri();
		final ECKey privateKey = job.getPrivateKey();
		final List<Service> addServices = job.getAddServices();
		final List<VerificationMethod> verificationMethods = job.getAddVerificationMethods();
		final List<VerificationMethod> addAuthentications = job.getAddAuthentications();
		final NetworkParameters params = BitcoinUtils.chainToNetworkParameters(chain);

		final ChainAndLocationData chainAndLocationData = NetworkUtils.getChainAndLocationData(chain, txHash,
				driver.getRpcClient(chain));
		final String txref = ChainAndLocationData.txrefEncode(chainAndLocationData);
		final String did = configs.getMethodPrefix() + BTCRUtils.stripTxref(txref);

		if (didContinuationUri != null) {
			log.debug("Storing the DID Continuation Document with URI: {}", didContinuationUri::toString);

			final DIDDocument didContinuationDocument = DIDDocument.builder().id(URI.create(did))
					.verificationMethods(verificationMethods).authenticationVerificationMethods(addAuthentications).services(addServices)
					.build();

			log.debug("Created DIDDocument for DID: {}", didContinuationDocument::getId);
			final DIDDocContinuation didContinuation = configs.getDidDocContinuation();

			try {
				didContinuation.storeDIDDocContinuation(didContinuationUri, didContinuationDocument);
			} catch (IOException e) {
				throw new RegistrationException(e.getMessage());
			}
		}

		final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
		final String privateKeyBase58 = Base58.encode(privateKey.getPrivKeyBytes());
		final Map<String, Object> jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey).toMap();
		jsonWebKey.values().removeAll(Collections.singleton(null));
		final String publicKeyDIDURL = BTCRUtils.identifierToPublicKeyDIDURL(did);

		final ECKey changeKey = job.getChangeKey();

		final String privateKeyWif2 = changeKey.getPrivateKeyAsWiF(params);
		final String privateKeyBase58_2 = Base58.encode(changeKey.getPrivKeyBytes());
		final Map<String, Object> jsonWebKey2 = ECKeyUtils.privateKeyToJWK(changeKey).toMap();
		jsonWebKey2.values().removeAll(Collections.singleton(null));

		final Map<String, Object> jsonKey = new LinkedHashMap<>();
		jsonKey.put("id", publicKeyDIDURL);
		jsonKey.put("type",ECKeyUtils.LD_KEY_TYPE);
		jsonKey.put("controller", did);
		jsonKey.put("privateKeyJwk", jsonWebKey);
		jsonKey.put("privateKeyBase58", privateKeyBase58);
		jsonKey.put("privateKeyWif", privateKeyWif);
		jsonKey.put("purpose", new String[]{"authentication", "assertionMethod"});

		final Map<String, Object> secret = new LinkedHashMap<>();
		final Map<String, Object> jsonKey2 = new LinkedHashMap<>();
		jsonKey2.put("type",ECKeyUtils.LD_KEY_TYPE);
		jsonKey2.put("privateKeyJwk", jsonWebKey2);
		jsonKey2.put("privateKeyBase58", privateKeyBase58_2);
		jsonKey2.put("privateKeyWif", privateKeyWif2);
		jsonKey2.put("purpose", new String[]{"update", "deactivate"});

		secret.put("verificationMethod", List.of(jsonKey, jsonKey2));

		// Registration is finished. Prepare the result.
		final Map<String, Object> methodMetadata = new LinkedHashMap<>();
		methodMetadata.put("createCompletionTime", BTCRUtils.getTimeStamp());
		methodMetadata.put("chain", chain);
		methodMetadata.put("transactionHash", txHash);
		methodMetadata.put("blockHeight", chainAndLocationData.getLocationData().getBlockHeight());
		methodMetadata.put("transactionPosition", chainAndLocationData.getLocationData().getTransactionPosition());
		methodMetadata.put("txoIndex", chainAndLocationData.getLocationData().getTxoIndex());
		methodMetadata.put("didContinuationUri", "" + didContinuationUri);
		methodMetadata.put("operation", "create");

		final CreateState state = CreateState.build();

		SetCreateStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Registration state for the job {} is completed", job::getJobId);
		log.debug("Registration is completed: {}", () -> job);
		log.debug("Registration State: {}", state::toString);

		driver.jobCompleted(job.getJobId());
		driver.getCreateStates().put(job.getJobId(), state);

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
			final Map<String, Object> jsonWebKey = ECKeyUtils.privateKeyToJWK(privateKey).toMap();
			final String privateKeyWif = privateKey.getPrivateKeyAsWiF(params);
			final String privateKeyBase58 = Base58.encode(privateKey.getPrivKeyBytes());
			final Map<String, Object> jsonKey = new LinkedHashMap<>();

			jsonWebKey.values().removeAll(Collections.singleton(null));
			jsonKey.put("type",ECKeyUtils.LD_KEY_TYPE);
			jsonKey.put("privateKeyJwk", jsonWebKey);
			jsonKey.put("privateKeyBase58", privateKeyBase58);
			jsonKey.put("privateKeyWif", privateKeyWif);
			secret.put("verificationMethod", jsonKey);
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

		SetBtcrCreateStateFinished.setStateFinished(state, did, secret);
		state.setMethodMetadata(methodMetadata);
		state.setJobId(job.getJobId());

		log.info("Deactivation state for the job {} is completed", job::getJobId);
		log.debug("Deactivation State: {}", state::toString);
		driver.jobCompleted(job.getJobId());
		driver.getDeactivateStates().put(job.getJobId(), state);
	}

}
