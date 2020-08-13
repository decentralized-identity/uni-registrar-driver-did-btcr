package uniregistrar.driver.did.btcr;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.Wallet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import uniregistrar.RegistrationException;
import uniregistrar.driver.AbstractDriver;
import uniregistrar.driver.Driver;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.funding.FundingService;
import uniregistrar.driver.did.btcr.funding.InMemoryFundingService;
import uniregistrar.driver.did.btcr.handlers.*;
import uniregistrar.driver.did.btcr.service.BitcoinJWalletAppKit;
import uniregistrar.driver.did.btcr.service.ExecutorProvider;
import uniregistrar.driver.did.btcr.service.UTXOProducer;
import uniregistrar.driver.did.btcr.state.SetBtcrRegisterStateFailed;
import uniregistrar.driver.did.btcr.transaction.BitcoinConfirmationTracker;
import uniregistrar.driver.did.btcr.util.Configurator;
import uniregistrar.driver.did.btcr.util.ErrorMessages;
import uniregistrar.driver.did.btcr.util.NetworkUtils;
import uniregistrar.driver.did.btcr.util.ParsingUtils;
import uniregistrar.driver.did.btcr.util.validators.*;
import uniregistrar.request.DeactivateRequest;
import uniregistrar.request.RegisterRequest;
import uniregistrar.request.UpdateRequest;
import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

public class DidBtcrDriver extends AbstractDriver implements Driver {

	private static final Logger log = LogManager.getLogger(DidBtcrDriver.class);
	// Keep registration states across components
	private final ConcurrentHashMap<String, RegisterState> registerStates;
	// Keep update states across components
	private final ConcurrentHashMap<String, UpdateState> updateStates;
	// Keep deactivation states across components
	private final ConcurrentHashMap<String, DeactivateState> deactivateStates;
	// Jobs waiting for required confirmation depth/resource to be completed
	private final BlockingQueue<DidBtcrJob> completionQueue;
	// Created but not yet completed jobs
	private final Map<String, DidBtcrJob> unconfirmedJobs;
	// Initial configurations used for this driver
	private DriverConfigs configs = null;
	private BitcoindRPCBitcoinConnection rpcClientTestNet;
	private BitcoindRPCBitcoinConnection rpcClientMainNet;
	private BitcoindRPCBitcoinConnection rpcClientRegtest;
	private volatile boolean online;
	private BitcoinJWalletAppKit walletServiceMainnet = null;
	private BitcoinJWalletAppKit walletServiceTestnet = null;
	private BitcoinJWalletAppKit walletServiceRegtest = null;
	private RegisterHandler registerHandler;
	private UpdateHandler updateHandler;
	private CompletionHandler completionHandler = null;
	private DeactivationHandler deactivationHandler;

	private FundingService fundingServiceRegtest;
	private FundingService fundingServiceTestnet;
	private FundingService fundingServiceMainnet;

	private UTXOProducer utxoProducerRegtest;
	private UTXOProducer utxoProducerTestnet;
	private UTXOProducer utxoProducerMainnet;

	private volatile Wallet utxoWalletMainnet;
	private volatile Wallet utxoWalletTestnet;
	private volatile Wallet utxoWalletRegtestnet;

	private File utxoWalletMainnetFile;
	private File utxoWalletTestnetFile;
	private File utxoWalletRegtestnetFile;
	private volatile Context contextMainnet;
	private volatile Context contextTestnet;
	private volatile Context contextRegtest;

	private BitcoinConfirmationTracker confirmationTrackerRegtest;
	private BitcoinConfirmationTracker confirmationTrackerTestnet;
	private BitcoinConfirmationTracker confirmationTrackerMainnet;

	private Map<String, Object> propsToShow = null;

	public DidBtcrDriver() {
		this(new Properties());
	}

	public DidBtcrDriver(Properties props) {
		Thread.currentThread().setName("DidBtcrDriver-MainThread");
		Preconditions.checkNotNull(props, "Driver properties cannot be null!");
		log.debug("Creating new uniregistrar.driver.did.btcr.DidBtcrDriver with given properties {}",
				() -> StringUtils.join(props));
		try {
			configs = Configurator.getDriverProps(props);
		} catch (ConfigurationException e) {
			log.error(e.getMessage());
			System.exit(1);
		}
		registerStates = new ConcurrentHashMap<>();
		updateStates = new ConcurrentHashMap<>();
		deactivateStates = new ConcurrentHashMap<>();
		completionQueue = new LinkedBlockingDeque<>();
		unconfirmedJobs = new ConcurrentHashMap<>();

		try {
			initDriver();
		} catch (ConfigurationException e) {
			log.error(e.getMessage());
			System.exit(1);
		}
	}

	private void initDriver() throws ConfigurationException {

		log.info("Initializing the Btcr Driver...");

		final boolean mainnet = configs.isRunMainnet();
		final boolean testnet = configs.isRunTestnet();
		final boolean regtest = configs.isRunRegtest();

		Preconditions.checkNotNull(configs, "Driver is not configured yet!");
		ConfigurationValidator.preStartValidate(configs);

		if (!configs.isRunRegtest() && !configs.isRunMainnet() && !configs.isRunTestnet()) {
			throw new ConfigurationException("No chain is set to active!");
		}

		// Add a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutDown));

		log.debug("Getting rpc connections...");
		if (mainnet) {
			try {
				rpcClientMainNet = NetworkUtils.getBitcoindRpcClient(configs, Chain.MAINNET);
				String mainnetCert = configs.getCertificateMainnet();
				if (mainnetCert != null) {
					rpcClientMainNet.getBitcoindRpcClient().setSslSocketFactory(
							NetworkUtils.getSslSocketFactory(ParsingUtils.getCertificateString(mainnetCert)));
				}
				contextMainnet = new Context(MainNetParams.get());
			} catch (MalformedURLException e) {
				throw new ConfigurationException(e);
			}
		}
		if (testnet) {
			try {
				rpcClientTestNet = NetworkUtils.getBitcoindRpcClient(configs, Chain.TESTNET);
				String testnetCert = configs.getCertificateTestnet();
				if (testnetCert != null) {
					rpcClientTestNet.getBitcoindRpcClient().setSslSocketFactory(
							NetworkUtils.getSslSocketFactory(ParsingUtils.getCertificateString(testnetCert)));
				}
				contextTestnet = new Context(TestNet3Params.get());
			} catch (MalformedURLException e) {
				throw new ConfigurationException(e);
			}
		}
		if (regtest) {
			try {
				rpcClientRegtest = NetworkUtils.getBitcoindRpcClient(configs, Chain.REGTESTNET);
				contextRegtest = new Context(RegTestParams.get());
			} catch (MalformedURLException e) {
				throw new ConfigurationException(e);
			}
		}

		log.debug("Open wallet services...");

		CompletableFuture<Void> openMainnet = null;
		CompletableFuture<Void> openTestnet = null;
		CompletableFuture<Void> openRegtest = null;

		if (mainnet) {
			openMainnet = CompletableFuture.runAsync(() -> openWalletService(Chain.MAINNET));
		}
		if (testnet) {
			openTestnet = CompletableFuture.runAsync(() -> openWalletService(Chain.TESTNET));
		}
		if (regtest) {
			openRegtest = CompletableFuture.runAsync(() -> openWalletService(Chain.REGTESTNET));
		}
		log.info("Setting driver state to online...");

		if (mainnet) {
			while (!openMainnet.isDone()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					throw new ConfigurationException(e.getMessage());
				}
			}

			if (openMainnet.isCompletedExceptionally()) {
				throw new ConfigurationException("Cannot start the mainnet walletAppKit!");
			}

			confirmationTrackerMainnet = new BitcoinConfirmationTracker(this, Chain.MAINNET, configs.getRequiredDepth(),
					rpcClientMainNet);
			if (configs.getMainnetCheckInterval() > 0) {
				confirmationTrackerMainnet.start(configs.getMainnetCheckInterval());
			}
			fundingServiceMainnet = new InMemoryFundingService(this, Chain.MAINNET);
		}

		if (testnet) {
			while (!openTestnet.isDone()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					throw new ConfigurationException(e.getMessage());
				}
			}

			if (openTestnet.isCompletedExceptionally()) {
				throw new ConfigurationException("Cannot start the testnet walletAppKit!");
			}

			confirmationTrackerTestnet = new BitcoinConfirmationTracker(this, Chain.TESTNET, configs.getRequiredDepth(),
					rpcClientTestNet);
			if (configs.getTestnetCheckInterval() > 0) {
				confirmationTrackerTestnet.start(configs.getTestnetCheckInterval());
			}
			fundingServiceTestnet = new InMemoryFundingService(this, Chain.TESTNET);
		}

		if (regtest) {
			while (!openRegtest.isDone()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					throw new ConfigurationException(e.getMessage());
				}
			}

			if (openRegtest.isCompletedExceptionally()) {
				throw new ConfigurationException("Cannot start the regtest walletAppKit!");
			}

			confirmationTrackerRegtest = new BitcoinConfirmationTracker(this, Chain.REGTESTNET,
					configs.getRequiredDepth(), rpcClientRegtest);
			if (configs.getRegtestCheckInterval() > 0) {
				confirmationTrackerRegtest.start(configs.getRegtestCheckInterval());
			}
			fundingServiceRegtest = new InMemoryFundingService(this, Chain.REGTESTNET);
		}

		registerHandler = new RegisterHandlerBtcr(this);
		updateHandler = new UpdateHandlerBtcr(this);
		deactivationHandler = new DeactivationHandlerBtcr(this);

		online = true;

		completionHandler = new CompletionHandlerBtcr(this);
		completionHandler.start();

		if (mainnet && configs.isActivateUTXOProducingMainnet()) {
			try {
				utxoWalletMainnetFile = new File(configs.getWalletPathMainnet() + "/mainnet_utxo_wallet");
				utxoWalletMainnet = walletServiceMainnet.creatOrLoadWallet(utxoWalletMainnetFile, false);
				walletServiceMainnet.addWallet(utxoWalletMainnet);
			} catch (Exception e) {
				throw new ConfigurationException(e);
			}
			utxoProducerMainnet = new UTXOProducer(this, Chain.MAINNET, walletServiceMainnet.getWallet(),
					configs.getUtxoUpkeepTarget(Chain.MAINNET), Coin.valueOf(configs.getTargetFundAmount()));
			utxoProducerMainnet.start();
		}

		if (testnet && configs.isActivateUTXOProducingTestnet()) {
			try {
				utxoWalletTestnetFile = new File(configs.getWalletPathTestnet() + "/testnet_utxo.wallet");
				utxoWalletTestnet = walletServiceTestnet.creatOrLoadWallet(utxoWalletTestnetFile, false);
				walletServiceTestnet.addWallet(utxoWalletTestnet);
			} catch (Exception e) {
				throw new ConfigurationException(e);
			}
			utxoProducerTestnet = new UTXOProducer(this, Chain.TESTNET, walletServiceTestnet.getWallet(),
					configs.getUtxoUpkeepTarget(Chain.TESTNET), Coin.valueOf(configs.getTargetFundAmount()));
			utxoProducerTestnet.start();
		}

		if (regtest && configs.isActivateUTXOProducingRegtest()) {
			try {
				utxoWalletRegtestnetFile = new File(configs.getWalletPathRegtest() + "/regtestnet_utxo.wallet");
				utxoWalletRegtestnet = walletServiceRegtest.creatOrLoadWallet(utxoWalletRegtestnetFile, false);
				walletServiceRegtest.addWallet(utxoWalletRegtestnet);
			} catch (Exception e) {
				throw new ConfigurationException(e);
			}
			utxoProducerRegtest = new UTXOProducer(this, Chain.REGTESTNET, walletServiceRegtest.getWallet(),
					configs.getUtxoUpkeepTarget(Chain.REGTESTNET), Coin.valueOf(configs.getTargetFundAmount()));
			utxoProducerRegtest.start();
		}

		ConfigurationValidator.postStartValidate(this, configs);
		configs.finalizeConfiguration();

	}

	private void shutDown() {
		log.info("Performing cleanup of DID BTCR Driver shutdown...");

		online = false;

		// Remove confirmation waiting keys to mitigate broken states

		log.info("There are {} waiting jobs!", unconfirmedJobs::size);

//		unconfirmedJobs.values().stream().parallel().forEach(j -> removeUtxoKey(j.getPrivateKey(), j.getChain()));

		if (completionHandler != null) {
			completionHandler.stop();
		}

		if (utxoProducerMainnet != null) {
			utxoProducerMainnet.stop();
		}

		if (utxoProducerTestnet != null) {
			utxoProducerTestnet.stop();
		}

		if (utxoProducerRegtest != null) {
			utxoProducerRegtest.stop();
		}

		if (utxoWalletMainnet != null) {
			try {
				Context.propagate(contextMainnet);
				utxoWalletMainnet.saveToFile(utxoWalletMainnetFile);
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		if (utxoWalletTestnet != null) {
			try {
				Context.propagate(contextTestnet);
				utxoWalletTestnet.saveToFile(utxoWalletTestnetFile);
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		if (utxoWalletRegtestnet != null) {
			try {
				Context.propagate(contextRegtest);
				utxoWalletRegtestnet.saveToFile(utxoWalletRegtestnetFile);
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}

		if (walletServiceMainnet != null) {
			walletServiceMainnet.stop();
		}
		if (walletServiceTestnet != null) {
			walletServiceTestnet.stop();
		}
		if (walletServiceRegtest != null) {
			walletServiceRegtest.stop();
		}

		log.info("Waiting for BTCR Driver to clean resources...");

		ExecutorProvider.shutDown(DriverConstants.SHUTDOWN_WAITING_TIME);

		try {
			TimeUnit.SECONDS.sleep(DriverConstants.SHUTDOWN_WAITING_TIME);
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}
	}

	private void openWalletService(Chain chain) {
		Preconditions.checkNotNull(chain, "Chain cannot be null!");
		log.info("Opening wallet for the chain {} ...", chain);
		switch (chain) {
		case MAINNET: {
			walletServiceMainnet = new BitcoinJWalletAppKit(this, chain, configs.getWalletPath(chain),
					configs.getWalletPrefix(chain), configs.getWalletKey(chain));
			walletServiceMainnet.setPeers(configs.getMainnetPeers());
			walletServiceMainnet.start();
			break;
		}
		case TESTNET: {
			walletServiceTestnet = new BitcoinJWalletAppKit(this, chain, configs.getWalletPath(chain),
					configs.getWalletPrefix(chain), configs.getWalletKey(chain));
			walletServiceTestnet.setPeers(configs.getTestnetPeers());
			walletServiceTestnet.start();
			break;
		}
		case REGTESTNET: {
			walletServiceRegtest = new BitcoinJWalletAppKit(this, chain, configs.getWalletPath(chain),
					configs.getWalletPrefix(chain), configs.getWalletKey(chain));
			walletServiceRegtest.setPeers(configs.getRegtestPeers());
			walletServiceRegtest.start();
			break;
		}
		default:
			throw new IllegalArgumentException("No such a chain!");
		}
	}

	public DidBtcrDriver(Map<String, Object> properties) {
		this(MapUtils.toProperties(properties));
	}

	public void removeUtxoKey(ECKey key, Chain chain) {
		boolean removed;
		switch (chain) {
		case MAINNET:
			Context.propagate(contextMainnet);
			removed = utxoWalletMainnet.removeKey(key);
			break;
		case TESTNET:
			Context.propagate(contextTestnet);
			removed = utxoWalletTestnet.removeKey(key);
			break;
		case REGTESTNET:
			Context.propagate(contextRegtest);
			removed = utxoWalletRegtestnet.removeKey(key);
			break;
		default:
			throw new IllegalArgumentException(ErrorMessages.UNKNOWN_CHAIN);
		}

		if (removed) {
			log.debug("Key with public-key hex {} is removed from the Wallet", key::getPublicKeyAsHex);
		} else {
			log.error("Failed to remove the key with public-key hex {} from the Wallet", key::getPublicKeyAsHex);
		}
	}

	public Wallet getUtxoWallet(Chain chain) {
		Preconditions.checkNotNull(chain);
		switch (chain) {
		case MAINNET:
			return utxoWalletMainnet;
		case TESTNET:
			return utxoWalletTestnet;
		case REGTESTNET:
			return utxoWalletRegtestnet;
		default:
			throw new IllegalArgumentException("No such chain!");
		}
	}

	@Override
	public RegisterState register(RegisterRequest registerRequest) throws RegistrationException {

		final String jobId = registerRequest.getJobId();
		log.debug("Request has job id of {}", () -> jobId == null ? "null" : jobId);

		// Check if job is in progress
		if (jobId != null && !jobId.isEmpty()) {
			if (registerStates.containsKey(jobId)) {
				log.debug("Job ID is found");
				// Check if job is finished
				final RegisterState retState = registerStates.get(jobId);
				if (registerStates.get(jobId).getDidState().get("state").equals("finished")) {
					log.debug(
							"Requested job is already finished. Removing it from the register states, returning its state: {}",
							() -> retState);
					registerStates.remove(jobId);
					return retState;
				} else if (registerStates.get(jobId).getDidState().get("state").equals("failed")) {
					log.debug("Requested job is failed. Removing it from the register states, returning its state: {}",
							() -> retState);
					registerStates.remove(jobId);
					return retState;
				}
				// This method double checks the confirmation state and will be removed after
				// stabilizing the confidence tracking
				else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) retState.getMethodMetadata().get("chain")))
								.checkConfirmationsWithJobID(jobId)) {
					DidBtcrJob job = unconfirmedJobs.get(jobId);
					CompletableFuture<Void> completeJob = CompletableFuture.runAsync(() -> {
						try {
							completionHandler.completeRegistration(job);
						} catch (IOException | RegistrationException e) {
							log.error("Cannot complete the registrations! {}", e::getMessage);
							jobFailed(job.getJobId(), e.getMessage());
						}
					});
					try {
						completeJob.get(1, TimeUnit.SECONDS);
					} catch (InterruptedException | TimeoutException | ExecutionException e) {
						log.error(e.getMessage());
						return registerStates.get(jobId);
					}
					if (registerStates.get(jobId).getDidState().get("state").equals("finished")) {
						log.debug("Job {} is completed with on-demand confirmation check triggering.", () -> jobId);
						return registerStates.remove(jobId);
					}
				} else {
					log.debug("Requested job is still in progress. Returning its current state: {}", () -> retState);
					return retState;
				}
			} else {
				throw new RegistrationException("Invalid JobID"); // Note: This will be same for the old jobIDs too
			}
		}

		try {
			RegisterRequestValidator.validate(registerRequest, configs);
		} catch (ValidationException e) {
			log.error(e.getMessage(), e);
			throw new RegistrationException(e.getMessage(), e);
		}

		log.debug("New registration request is received: {}", () -> registerRequest);

		final Map<String, Object> options = registerRequest.getOptions();

		final Chain chain = options == null || !options.containsKey("chain") ? Chain.TESTNET
				: Chain.fromString((String) registerRequest.getOptions().get("chain"));

		log.debug("Request  will be processed on chain {}", chain);

		final BitcoinJWalletAppKit walletService = getWalletService(chain);

		while (!online && !walletService.isOnline()) {
			log.debug("Wallet service for {} is not online yet. Waiting...", chain);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				throw new RegistrationException("Wallet is not ready, please try another time!");
			}
		}

		if (options != null && options.containsKey("fundingRequest")) {
			final Map<String, String> toRet = getFundingService(chain).askForFunding();
			RegisterState state = RegisterState.build();
			state.setDidState(null);
			state.setRegistrarMetadata(null);
			state.setJobId(null);
			state.setMethodMetadata(Collections.singletonMap("fundingInfo", toRet));
			return state;
		}

		try {
			return registerHandler.handle(registerRequest);
		} catch (RegistrationException e) {
			throw new RegistrationException(e.getMessage());
		}
	}

	@Override
	public UpdateState update(UpdateRequest updateRequest) throws RegistrationException {

		log.debug("New update request is received: {}", () -> updateRequest);
		final String jobId = updateRequest.getJobId();

		if (jobId != null && !jobId.isEmpty()) {
			log.debug("UpdateRequest has a jobId of: {}", jobId::toString);
			final UpdateState state = updateStates.get(jobId);
			if (updateStates.containsKey(jobId)) {
				log.debug("Job ID is found in update states!");
				if (updateStates.get(jobId).getDidState().get("state").equals("finished")) {
					log.debug(
							"Requested job is already finished. Removing it from the update states, returning its state: {}",
							() -> state);
					updateStates.remove(jobId);
					return state;
				} else if (updateStates.get(jobId).getDidState().get("state").equals("failed")) {
					log.debug("Requested job is failed. Removing it from the register states, returning its state: {}",
							() -> state);
					updateStates.remove(jobId);
					return state;
				} else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) state.getMethodMetadata().get("chain")))
								.checkConfirmationsWithJobID(jobId)) {
					DidBtcrJob job = unconfirmedJobs.get(jobId);
					CompletableFuture<Void> completeJob = CompletableFuture.runAsync(() -> {
						try {
							completionHandler.completeUpdate(job);
						} catch (IOException | RegistrationException e) {
							log.error("Cannot complete the registrations! {}", e::getMessage);
							jobFailed(job.getJobId(), e.getMessage());
						}
					});
					try {
						completeJob.get(1, TimeUnit.SECONDS);
					} catch (InterruptedException | TimeoutException | ExecutionException e) {
						log.error(e.getMessage());
						return updateStates.get(jobId);
					}
					log.debug("TX is already confirmed, sending it for the completion!");
					completionReady(jobId);
					if (updateStates.get(jobId).getDidState().get("state").equals("finished")) {
						log.debug("Job {} is completed with the fallback triggering.", () -> jobId);
						return updateStates.remove(jobId);
					}
				} else {
					log.debug("Requested Update operation is still in progress. Returning its current state: {}",
							() -> state);
					return state;
				}
			} else {
				throw new RegistrationException("Invalid JobID"); // Note: This will be same for the old jobIDs too
			}
		}

		try {
			UpdateRequestValidator.validate(updateRequest, configs);
		} catch (ValidationException e) {
			log.error(e.getMessage(), e);
			throw new RegistrationException(e.getMessage(), e);
		}

		final Chain chain = updateRequest.getOptions() == null ? Chain.TESTNET
				: Chain.fromString((String) updateRequest.getOptions().get("chain"));

		log.debug("UpdateRequest  will be processed on chain {}", chain);

		if (chain == null) {
			throw new RegistrationException("Unknown chain value!");
		}

		final BitcoinJWalletAppKit walletService = getWalletService(chain);

		while (!online && !walletService.isOnline()) {
			log.debug("Wallet service for {} is not online yet. Waiting...", chain);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				throw new RegistrationException("Wallet is not ready, please try another time!");
			}
		}

		try {
			return updateHandler.handle(updateRequest);
		} catch (RegistrationException e) {
			log.error(e);
			throw new RegistrationException(e.getMessage());
		}
	}

	@Override
	public DeactivateState deactivate(DeactivateRequest deactivateRequest) throws RegistrationException {
		final String jobId = deactivateRequest.getJobId();
		log.debug("Starting deactivation process for the job: {}", () -> jobId == null ? "new (no-id)" : jobId);

		if (jobId != null && !jobId.isEmpty()) {
			log.debug("Deactivation Request has a jobId of: {}", jobId::toString);
			if (deactivateStates.containsKey(jobId)) {
				log.debug("Job ID is found in deactivation states");
				final DeactivateState state = deactivateStates.get(jobId);
				if (deactivateStates.get(jobId).getDidState().get("state").equals("finished")) {
					log.debug(
							"Requested job is already finished. Removing it from the deactivation states, returning its state: {}",
							() -> state);
					deactivateStates.remove(jobId);
					return state;
				} else if (deactivateStates.get(jobId).getDidState().get("state").equals("failed")) {
					log.debug("Requested job is failed. Removing it from the register states, returning its state: {}",
							() -> state);
					deactivateStates.remove(jobId);
					return state;
				} else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) state.getMethodMetadata().get("chain")))
								.checkConfirmationsWithJobID(jobId)) {
					log.debug("TX is already confirmed, sending it for the completion!");
					DidBtcrJob job = unconfirmedJobs.get(jobId);
					CompletableFuture<Void> completeJob = CompletableFuture.runAsync(() -> {
						try {
							completionHandler.completeDeactivate(job);
						} catch (IOException | RegistrationException e) {
							log.error("Cannot complete the registrations! {}", e::getMessage);
							jobFailed(job.getJobId(), e.getMessage());
						}
					});
					try {
						completeJob.get(1, TimeUnit.SECONDS);
					} catch (InterruptedException | TimeoutException | ExecutionException e) {
						log.error(e.getMessage());
						return deactivateStates.get(jobId);
					}
					if (registerStates.get(jobId).getDidState().get("state").equals("finished")) {
						log.debug("Job {} is completed with the fallback triggering.", () -> jobId);
						return deactivateStates.remove(jobId);
					}
				} else {
					return deactivateStates.get(jobId);
				}
			} else {
				throw new RegistrationException("Invalid JobID"); // Note: This will be same for the old jobIDs too
			}
		}

		try {
			DeactivationRequestValidator.validate(deactivateRequest, configs);
		} catch (ValidationException e) {
			log.error(e.getMessage(), e);
			throw new RegistrationException(e.getMessage(), e);
		}

		log.debug("New deactivation request is received: {}", () -> deactivateRequest);

		final Chain chain = deactivateRequest.getOptions() == null ? Chain.TESTNET
				: Chain.fromString((String) deactivateRequest.getOptions().get("chain"));

		log.debug("Deactivation Request will be processed on chain {}", chain);

		if (chain == null) {
			throw new RegistrationException("Unknown chain value!");
		}

		final BitcoinJWalletAppKit walletService = getWalletService(chain);

		while (!online && !walletService.isOnline()) {
			log.debug("Wallet service for {} is not online yet. Waiting...", chain);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				throw new RegistrationException("Wallet is not ready, please try another time!");
			}
		}

		try {
			return deactivationHandler.handle(deactivateRequest);
		} catch (RegistrationException e) {
			throw new RegistrationException(e.getMessage());
		}
	}

	@Override
	public Map<String, Object> properties() {

		if(propsToShow == null){
			final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			propsToShow = mapper.convertValue(configs, new TypeReference<Map<String, Object>>() {});
			propsToShow.put("certificateMainnet","................................");
			propsToShow.put("certificateTestnet", "................................");
			propsToShow.put("rpcUrlMainnet","................................");
			propsToShow.put("rpcUrlTestnet", "................................");
		}

		return propsToShow;
	}

	public void completionReady(String jobId) {
		if (jobId == null) {
			log.debug("Internal ERROR: null passed to completion ready method!");
		}
		log.debug("Job: {} is ready for completion", () -> jobId);
		DidBtcrJob job = unconfirmedJobs.get(jobId);
		boolean inQueue = completionQueue.add(job);
		log.debug("Job: {} is accepted to completion queue {}", () -> jobId, () -> inQueue);
	}

	public BitcoinConfirmationTracker getBitcoinConfirmationTracker(Chain chain) {
		Preconditions.checkNotNull(chain, "Chain cannot be null!");
		switch (chain) {
		case MAINNET:
			return confirmationTrackerMainnet;
		case TESTNET:
			return confirmationTrackerTestnet;
		case REGTESTNET:
			return confirmationTrackerRegtest;
		default:
			throw new IllegalArgumentException("No such chain!");
		}
	}

	public void jobFailed(String jobId, String reason) {
		DidBtcrJob failed = unconfirmedJobs.remove(jobId);
		JobType jobType = failed.getJobType();
		String initTime = ParsingUtils.getTimeStamp(failed.getCreationTime());
		log.info("{} job with id of {} is failed!", () -> jobType, () -> jobId);
		log.info("Job was created at {}", () -> initTime);
		if (jobType == JobType.REGISTER) {
			RegisterState state = registerStates.get(jobId);
			SetBtcrRegisterStateFailed.setStateFail(state, reason, initTime);
			registerStates.put(jobId, state);
		} else if (jobType == JobType.UPDATE) {
			UpdateState state = updateStates.get(jobId);
			SetBtcrRegisterStateFailed.setStateFail(state, reason, initTime);
			updateStates.put(jobId, state);
		} else {
			DeactivateState state = deactivateStates.get(jobId);
			SetBtcrRegisterStateFailed.setStateFail(state, reason, initTime);
			deactivateStates.put(jobId, state);
		}
	}

	public BitcoinJWalletAppKit getWalletService(Chain chain) {
		Preconditions.checkNotNull(chain, "Chain cannot be null!");
		switch (chain) {
		case MAINNET:
			return walletServiceMainnet;
		case TESTNET:
			return walletServiceTestnet;
		case REGTESTNET:
			return walletServiceRegtest;
		default:
			throw new IllegalArgumentException("No such chain!");
		}
	}

	public FundingService getFundingService(Chain chain) {
		Preconditions.checkNotNull(chain, "Chain cannot be null!");
		switch (chain) {
		case MAINNET:
			return fundingServiceMainnet;
		case TESTNET:
			return fundingServiceTestnet;
		case REGTESTNET:
			return fundingServiceRegtest;
		default:
			throw new IllegalArgumentException("No such chain!");
		}
	}

	public void jobCompleted(String jobId) {
		unconfirmedJobs.remove(jobId);
		log.debug("Job: {} is completed!", () -> jobId);
	}

	public void addNewJob(DidBtcrJob job) {
		log.debug("New job sent to confirmation queue...");
		unconfirmedJobs.put(job.getJobId(), job);
		log.debug("Job with id {} is in unconfirmed job queue now!", job::getJobId);
	}

	public BitcoindRPCBitcoinConnection getRpcClient(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return rpcClientMainNet;
		case TESTNET:
			return rpcClientTestNet;
		case REGTESTNET:
			return rpcClientRegtest;
		default:
			throw new IllegalArgumentException(ErrorMessages.UNKNOWN_CHAIN);
		}
	}

	public Context getContext(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return contextMainnet;
		case TESTNET:
			return contextTestnet;
		case REGTESTNET:
			return contextRegtest;
		default:
			throw new IllegalArgumentException(ErrorMessages.UNKNOWN_CHAIN);
		}
	}

	public DriverConfigs getConfigs() {
		return configs;
	}

	public boolean isOnline() {
		return online;
	}

	public BlockingQueue<DidBtcrJob> getCompletionQueue() {
		return completionQueue;
	}

	public Map<String, RegisterState> getRegisterStates() {
		return registerStates;
	}

	public Map<String, DeactivateState> getDeactivateStates() {
		return deactivateStates;
	}

	public Map<String, UpdateState> getUpdateStates() {
		return updateStates;
	}
}