package uniregistrar.driver.did.btcr;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.PassiveExpiringMap;
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
import uniregistrar.RegistrationException;
import uniregistrar.driver.Driver;
import uniregistrar.driver.did.btcr.enums.JobType;
import uniregistrar.driver.did.btcr.funding.BtcrFund;
import uniregistrar.driver.did.btcr.funding.FundingException;
import uniregistrar.driver.did.btcr.funding.FundingService;
import uniregistrar.driver.did.btcr.funding.InMemoryFundingService;
import uniregistrar.driver.did.btcr.handlers.*;
import uniregistrar.driver.did.btcr.service.BitcoinJWalletAppKit;
import uniregistrar.driver.did.btcr.service.ExecutorProvider;
import uniregistrar.driver.did.btcr.service.UTXOProducer;
import uniregistrar.driver.did.btcr.state.SetBtcrCreateStateFailed;
import uniregistrar.driver.did.btcr.transaction.BitcoinConfirmationTracker;
import uniregistrar.driver.did.btcr.util.Configurator;
import uniregistrar.driver.did.btcr.util.ErrorMessages;
import uniregistrar.driver.did.btcr.util.NetworkUtils;
import uniregistrar.driver.did.btcr.util.ParsingUtils;
import uniregistrar.driver.did.btcr.util.validators.*;
import uniregistrar.request.CreateRequest;
import uniregistrar.request.DeactivateRequest;
import uniregistrar.request.UpdateRequest;
import uniregistrar.state.CreateState;
import uniregistrar.state.DeactivateState;
import uniregistrar.state.UpdateState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class DidBtcrDriver implements Driver {

	private static final Logger log = LogManager.getLogger(DidBtcrDriver.class);
	// Keep registration states across components
	private final ConcurrentHashMap<String, CreateState> createStates;
	// Keep update states across components
	private final ConcurrentHashMap<String, UpdateState> updateStates;
	// Keep deactivation states across components
	private final ConcurrentHashMap<String, DeactivateState> deactivateStates;
	// Jobs waiting for required confirmation depth/resource to be completed
	private final BlockingQueue<DidBtcrJob> completionQueue;
	// Created but not yet completed jobs
	private final Map<String, DidBtcrJob> unconfirmedJobs;
	// Jobs with funding requirement
	private final Map<String, DidBtcrJob> fundingRequiredJobs;
	// Initial configurations used for this driver
	private DriverConfigs configs = null;
	private BitcoindRPCBitcoinConnection rpcClientTestNet;
	private BitcoindRPCBitcoinConnection rpcClientMainNet;
	private BitcoindRPCBitcoinConnection rpcClientRegtest;
	private volatile boolean online;
	private BitcoinJWalletAppKit walletServiceMainnet = null;
	private BitcoinJWalletAppKit walletServiceTestnet = null;
	private BitcoinJWalletAppKit walletServiceRegtest = null;
	private CreateHandler createHandler;
	private UpdateHandler updateHandler;
	private CompletionHandler completionHandler = null;
	private DeactivationHandler deactivationHandler;

	private FundingService fundingServiceRegtest;
	private FundingService fundingServiceTestnet;
	private FundingService fundingServiceMainnet;

	private UTXOProducer utxoProducerRegtest;
	private UTXOProducer utxoProducerTestnet;
	private UTXOProducer utxoProducerMainnet;

	private Wallet utxoWalletMainnet;
	private Wallet utxoWalletTestnet;
	private Wallet utxoWalletRegtestnet;

	private File utxoWalletMainnetFile;
	private File utxoWalletTestnetFile;
	private File utxoWalletRegtestnetFile;
	private Context contextMainnet;
	private Context contextTestnet;
	private Context contextRegtest;

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
		createStates = new ConcurrentHashMap<>();
		updateStates = new ConcurrentHashMap<>();
		deactivateStates = new ConcurrentHashMap<>();
		completionQueue = new LinkedBlockingDeque<>();
		unconfirmedJobs = new ConcurrentHashMap<>();
		fundingRequiredJobs = new PassiveExpiringMap<>(DriverConstants.DEFAULT_FUNDING_WAIT_TIME, TimeUnit.HOURS,
				new ConcurrentHashMap<>());

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
				contextMainnet = new Context(MainNetParams.get(), 10, DriverConstants.DEFAULT_TX_FEE_KB, false);
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
				contextTestnet = new Context(TestNet3Params.get(), 10, DriverConstants.DEFAULT_TX_FEE_KB, false);
			} catch (MalformedURLException e) {
				throw new ConfigurationException(e);
			}
		}
		if (regtest) {
			try {
				rpcClientRegtest = NetworkUtils.getBitcoindRpcClient(configs, Chain.REGTESTNET);
				contextRegtest = new Context(RegTestParams.get(), 10, DriverConstants.DEFAULT_TX_FEE_KB, false);
			} catch (MalformedURLException e) {
				throw new ConfigurationException(e);
			}
		}

		log.debug("Open wallet services...");

		CompletableFuture<Boolean> openMainnet = null;
		CompletableFuture<Boolean> openTestnet = null;
		CompletableFuture<Boolean> openRegtest = null;

		if (mainnet) {
			openMainnet = CompletableFuture.supplyAsync(() -> openWalletService(Chain.MAINNET));
		}
		if (testnet) {
			openTestnet = CompletableFuture.supplyAsync(() -> openWalletService(Chain.TESTNET));
		}
		if (regtest) {
			openRegtest = CompletableFuture.supplyAsync(() -> openWalletService(Chain.REGTESTNET));
		}
		log.info("Setting driver state to online...");

		if (mainnet) {
			while (!openMainnet.isDone()) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new ConfigurationException("ConfigurationException: ", e);
				}
			}

			if (openMainnet.isCompletedExceptionally() || !(openMainnet.getNow(false))) {
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
					Thread.currentThread().interrupt();
					throw new ConfigurationException(e.getMessage());
				}
			}

			if (openTestnet.isCompletedExceptionally() || !(openTestnet.getNow(false))) {
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
					Objects.requireNonNull(openTestnet).cancel(true);
					Thread.currentThread().interrupt();
					throw new ConfigurationException(e.getMessage());
				}
			}

			if (openRegtest.isCompletedExceptionally() || !(openRegtest.getNow(false))) {
				throw new ConfigurationException("Cannot start the regtest walletAppKit!");
			}

			confirmationTrackerRegtest = new BitcoinConfirmationTracker(this, Chain.REGTESTNET,
					configs.getRequiredDepth(), rpcClientRegtest);
			if (configs.getRegtestCheckInterval() > 0) {
				confirmationTrackerRegtest.start(configs.getRegtestCheckInterval());
			}
			fundingServiceRegtest = new InMemoryFundingService(this, Chain.REGTESTNET);
		}

		createHandler = new CreateHandlerBtcr(this);
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

		log.info("There are {} waiting jobs!", unconfirmedJobs::size);

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
			log.error("InterruptedException: ", e);
			Thread.currentThread().interrupt();
		}
	}

	private boolean canLockFile(String filePath, int waitSec) {
		for (int i = 1; i <= waitSec; i++) {
			try (FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);
			     FileChannel channel = fileOutputStream.getChannel()) {

				FileLock lock = channel.tryLock();
				lock.release();
				return true;

			} catch (OverlappingFileLockException e) {
				log.warn("File '{}' is already locked!", filePath);
				log.debug("Lock attempt number: {}, remaining attempts: {}, rate: 1 attempt per second.", i, waitSec - i);
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException ie) {
					log.error("Interrupted: ", ie);
					Thread.currentThread().interrupt();
				}

			} catch (IOException e) {
				log.error("IOException: ", e);
			}
		}

		return false;
	}

	private boolean openWalletService(Chain chain) {
		Preconditions.checkNotNull(chain, "Chain cannot be null!");
		log.info("Opening wallet for the chain {} ...", chain);

		String walletPath = configs.getWalletPath(chain);
		String walletPrefix = configs.getWalletPrefix(chain);
		Path fullPathWallet = Paths.get(walletPath, walletPrefix + ".wallet");
		Path fullPathChain = Paths.get(walletPath, walletPrefix + ".spvchain");

		if(Files.exists(fullPathWallet)) {
			boolean wLock = canLockFile(fullPathWallet.toString(), 10);
			if (!wLock) {
				log.error("Lock for the wallet file of {} cannot acquired!", chain);
				return false;
			}
		}

		if(Files.exists(fullPathChain)) {
			boolean cLock = canLockFile(fullPathChain.toString(), 10);
			if (!cLock) {
				log.error("Lock for the chain file of {} cannot acquired!", chain);
				return false;
			}
		}
		switch (chain) {
			case MAINNET: {

				walletServiceMainnet = new BitcoinJWalletAppKit(this, chain, walletPath,
						walletPrefix, configs.getWalletKey(chain));
				walletServiceMainnet.setPeers(configs.getMainnetPeers());
				walletServiceMainnet.start();
				break;
			}
			case TESTNET: {
				walletServiceTestnet = new BitcoinJWalletAppKit(this, chain, walletPath,
						walletPrefix, configs.getWalletKey(chain));
				walletServiceTestnet.setPeers(configs.getTestnetPeers());
				walletServiceTestnet.start();
				break;
			}
			case REGTESTNET: {
				walletServiceRegtest = new BitcoinJWalletAppKit(this, chain, walletPath,
						walletPrefix, configs.getWalletKey(chain));
				walletServiceRegtest.setPeers(configs.getRegtestPeers());
				walletServiceRegtest.start();
				break;
			}
			default:
				throw new IllegalArgumentException("No such a chain!");
		}
		return true;
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

	public Optional<DidBtcrJob> getAndRemoveFundingRequiredJob(String jobId) {
		return Optional.ofNullable(jobId != null && fundingRequiredJobs.containsKey(jobId) ? fundingRequiredJobs.remove(jobId) : null);
	}

	@Override
	public CreateState create(CreateRequest createRequest) throws RegistrationException {

		final String jobId = createRequest.getJobId();
		log.debug("Request has job id of {}", () -> jobId == null ? "null" : jobId);

		final Map<String, Object> options = createRequest.getOptions();

		final Chain chain = options == null || !options.containsKey("chain") ? Chain.TESTNET
				: Chain.fromString((String) createRequest.getOptions().get("chain"));

		// Check if job is in progress
		if (jobId != null && !jobId.isEmpty()) {
			if (createStates.containsKey(jobId)) {
				log.debug("Job ID is found");
				// Check if job is finished
				final CreateState retState = createStates.get(jobId);
				if (createStates.get(jobId).getDidState().get("state").equals("finished")) {
					log.debug(
							"Requested job is already finished. Removing it from the create states, returning its state: {}",
							() -> retState);
					createStates.remove(jobId);
					return retState;
				} else if (createStates.get(jobId).getDidState().get("state").equals("failed")) {
					log.debug("Requested job is failed. Removing it from the create states, returning its state: {}",
							() -> retState);
					createStates.remove(jobId);
					return retState;
				}
				// This method double checks the confirmation state and will be removed after
				// stabilizing the confidence tracking
				else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) retState.getDidDocumentMetadata().get("chain")))
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
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						return createStates.get(jobId);
					}
					if (createStates.get(jobId).getDidState().get("state").equals("finished")) {
						log.debug("Job {} is completed with on-demand confirmation check triggering.", () -> jobId);
						return createStates.remove(jobId);
					}
				} else {
					log.debug("Requested job is still in progress. Returning its current state: {}", () -> retState);
					return retState;
				}
			} else if (fundingRequiredJobs.containsKey(jobId)) {
				log.debug("{} is in expected funds", () -> jobId);
				FundingService fundingService = getFundingService(chain);
				DidBtcrJob job = fundingRequiredJobs.get(jobId);
				try {
					CreateRequestValidator.validate(createRequest, configs);
					BtcrFund fund = fundingService.getExpectedFund(jobId, job.isRotateKey());
					return createHandler.handle(createRequest, fund);
				} catch (FundingException | ValidationException e) {
					throw new RegistrationException(e.getMessage());
				}

			} else {
				throw new RegistrationException("Invalid JobID"); // Note: This will be same for the old jobIDs too
			}
		}

		try {
			CreateRequestValidator.validate(createRequest, configs);
		} catch (ValidationException e) {
			log.error(e.getMessage(), e);
			throw new RegistrationException(e.getMessage(), e);
		}

		log.debug("New registration request is received: {}", () -> createRequest);

		log.debug("Request  will be processed on chain {}", chain);

		final BitcoinJWalletAppKit walletService = getWalletService(chain);

		while (!online && !walletService.isOnline()) {
			log.debug("Wallet service for {} is not online yet. Waiting...", chain);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				Thread.currentThread().interrupt();
				throw new RegistrationException("Wallet is not ready, please try another time!");
			}
		}

		if (options != null && options.containsKey("fundingRequest")) {
			final Map<String, String> toRet = getFundingService(chain).askForFunding();
			CreateState state = CreateState.build();
			state.setDidState(null);
			state.setDidRegistrationMetadata(null);
			state.setJobId(null);
			state.setDidDocumentMetadata(Collections.singletonMap("fundingInfo", toRet));
			return state;
		}

		try {
			return createHandler.handle(createRequest, null);
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
					log.debug("Requested job is failed. Removing it from the create states, returning its state: {}",
							() -> state);
					updateStates.remove(jobId);
					return state;
				} else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) state.getDidDocumentMetadata().get("chain")))
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
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
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
				Thread.currentThread().interrupt();
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
					log.debug("Requested job is failed. Removing it from the create states, returning its state: {}",
							() -> state);
					deactivateStates.remove(jobId);
					return state;
				} else if (getBitcoinConfirmationTracker(
						Chain.fromString((String) state.getDidDocumentMetadata().get("chain")))
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
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						return deactivateStates.get(jobId);
					}
					if (createStates.get(jobId).getDidState().get("state").equals("finished")) {
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
				Thread.currentThread().interrupt();
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

		if (propsToShow == null) {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			propsToShow = mapper.convertValue(configs, new TypeReference<Map<String, Object>>() {
			});
			propsToShow.put("certificateMainnet", "................................");
			propsToShow.put("certificateTestnet", "................................");
			propsToShow.put("rpcUrlMainnet", "................................");
			propsToShow.put("rpcUrlTestnet", "................................");
		}

		return Collections.unmodifiableMap(propsToShow);
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
		if (jobType == JobType.CREATE) {
			CreateState state = createStates.get(jobId);
			SetBtcrCreateStateFailed.setStateFail(state, reason, initTime);
			createStates.put(jobId, state);
		} else if (jobType == JobType.UPDATE) {
			UpdateState state = updateStates.get(jobId);
			SetBtcrCreateStateFailed.setStateFail(state, reason, initTime);
			updateStates.put(jobId, state);
		} else {
			DeactivateState state = deactivateStates.get(jobId);
			SetBtcrCreateStateFailed.setStateFail(state, reason, initTime);
			deactivateStates.put(jobId, state);
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

	public void jobCompleted(String jobId) {
		unconfirmedJobs.remove(jobId);
		log.info("Job: {} is completed!", () -> jobId);
	}

	public void addFundingRequiredJob(DidBtcrJob job) {
		fundingRequiredJobs.put(job.getJobId(), job);
		log.info("Job {} waiting for funding!", job::getJobId);
	}

	public void addNewJob(DidBtcrJob job) {
		log.info("New job sent to confirmation queue...");
		unconfirmedJobs.put(job.getJobId(), job);
		log.info("Job with id {} is in unconfirmed job queue now!", job::getJobId);
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

	public Map<String, CreateState> getCreateStates() {
		return createStates;
	}

	public Map<String, DeactivateState> getDeactivateStates() {
		return deactivateStates;
	}

	public Map<String, UpdateState> getUpdateStates() {
		return updateStates;
	}
}
