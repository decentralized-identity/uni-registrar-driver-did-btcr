package uniregistrar.driver.did.btcr;

import java.util.List;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.script.Script;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.diddoccontinuation.DIDDocContinuation;
import uniregistrar.driver.did.btcr.diddoccontinuation.DIDDocFileNameStrategy;
import uniregistrar.driver.did.btcr.util.ErrorMessages;

public class DriverConfigs {
	private String rpcUrlMainnet;
	private String rpcUrlTestnet;
	private String rpcUrlRegtest;
	private int regtestCheckInterval;
	private int testnetCheckInterval;
	private int mainnetCheckInterval;
	private String methodPrefix;
	private String testnetPrefix;
	private String mainnetPrefix;
	private String regtestPrefix;
	@JsonIgnore
	private DIDDocContinuation didDocContinuation;
	@JsonIgnore
	private DIDDocFileNameStrategy didDocFileNameStrategy;
	private int requiredDepth;
	private long targetFundAmount;
	private String basePath;
	private String baseUri;
	private int utxoUpkeepRegtest;
	private int utxoUpkeepTestnet;
	private int utxoUpkeepMainnet;
	@JsonIgnore
	private ECKey walletKeyRegtest;
	@JsonIgnore
	private ECKey walletKeyTestnet;
	@JsonIgnore
	private ECKey walletKeyMainnet;
	private String walletPathRegtest;
	private String walletPathTestnet;
	private String walletPathMainnet;
	private String walletPrefixRegtest;
	private String walletPrefixTestnet;
	private String walletPrefixMainnet;
	private List<PeerAddress> regtestPeers;
	private List<PeerAddress> testnetPeers;
	private List<PeerAddress> mainnetPeers;
	private Script.ScriptType prefScriptType;
	private boolean runMainnet;
	private boolean runTestnet;
	private boolean runRegtest;
	private BitcoinClientID bitcoinClientID;
	private boolean activateUTXOProducingMainnet;
	private boolean activateUTXOProducingTestnet;
	private boolean activateUTXOProducingRegtest;
	private boolean overWriteDidDocs;
	private boolean followDeactivate;
	private boolean configured = false;

	public void finalizeConfiguration() {
		if (!configured) {
			configured = true;
		}
	}

	public boolean isOverWriteDidDocs() {
		return overWriteDidDocs;
	}

	public void setOverWriteDidDocs(boolean overWriteDidDocs) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.overWriteDidDocs = overWriteDidDocs;
	}

	public boolean isFollowDeactivate() {
		return followDeactivate;
	}

	public void setFollowDeactivate(boolean followDeactivate) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.followDeactivate = followDeactivate;
	}

	public Script.ScriptType getPrefScriptType() {
		return prefScriptType;
	}

	public void setPrefScriptType(Script.ScriptType prefScriptType) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.prefScriptType = Preconditions.checkNotNull(prefScriptType, ErrorMessages.SCRIPT_TYPE_NULL);
	}

	public String getRpcUrl(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return rpcUrlMainnet;
		case TESTNET:
			return rpcUrlTestnet;
		case REGTESTNET:
			return rpcUrlRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public boolean isActivateUTXOProducingMainnet() {
		return activateUTXOProducingMainnet;
	}

	public void setActivateUTXOProducingMainnet(boolean activateUTXOProducingMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.activateUTXOProducingMainnet = activateUTXOProducingMainnet;
	}

	public boolean isActivateUTXOProducingTestnet() {
		return activateUTXOProducingTestnet;
	}

	public void setActivateUTXOProducingTestnet(boolean activateUTXOProducingTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.activateUTXOProducingTestnet = activateUTXOProducingTestnet;
	}

	public boolean isActivateUTXOProducingRegtest() {
		return activateUTXOProducingRegtest;
	}

	public void setActivateUTXOProducingRegtest(boolean activateUTXOProducingRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.activateUTXOProducingRegtest = activateUTXOProducingRegtest;
	}

	public BitcoinClientID getBitcoinClientID() {
		return bitcoinClientID;
	}

	public void setBitcoinClientID(BitcoinClientID bitcoinClientID) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.bitcoinClientID = bitcoinClientID;
	}

	public String getRpcUrlMainnet() {
		return rpcUrlMainnet;
	}

	public void setRpcUrlMainnet(String rpcUrlMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.rpcUrlMainnet = rpcUrlMainnet;
	}

	public String getRpcUrlTestnet() {
		return rpcUrlTestnet;
	}

	public void setRpcUrlTestnet(String rpcUrlTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.rpcUrlTestnet = rpcUrlTestnet;
	}

	public String getRpcUrlRegtest() {
		return rpcUrlRegtest;
	}

	public void setRpcUrlRegtest(String rpcUrlRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.rpcUrlRegtest = rpcUrlRegtest;
	}

	public int getCheckInterval(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		if (chain == Chain.MAINNET) {
			return mainnetCheckInterval;
		} else if (chain == Chain.TESTNET) {
			return testnetCheckInterval;
		} else if (chain == Chain.REGTESTNET) {
			return regtestCheckInterval;
		}
		return 0;
	}

	public int getRegtestCheckInterval() {
		return regtestCheckInterval;
	}

	public void setRegtestCheckInterval(int regtestCheckInterval) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.regtestCheckInterval = regtestCheckInterval;
	}

	public int getTestnetCheckInterval() {
		return testnetCheckInterval;
	}

	public void setTestnetCheckInterval(int testnetCheckInterval) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.testnetCheckInterval = testnetCheckInterval;
	}

	public int getMainnetCheckInterval() {
		return mainnetCheckInterval;
	}

	public void setMainnetCheckInterval(int mainnetCheckInterval) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.mainnetCheckInterval = mainnetCheckInterval;
	}

	public String getMethodPrefix() {
		return methodPrefix;
	}

	public void setMethodPrefix(String methodPrefix) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.methodPrefix = Preconditions.checkNotNull(methodPrefix, ErrorMessages.METHOD_PREFIX_NULL);
	}

	public String getChainPrefix(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return mainnetPrefix;
		case TESTNET:
			return testnetPrefix;
		case REGTESTNET:
			return regtestPrefix;
		default:
			throw new IllegalArgumentException(ErrorMessages.UNKNOWN_CHAIN);
		}
	}

	public String getTestnetPrefix() {
		return testnetPrefix;
	}

	public void setTestnetPrefix(String testnetPrefix) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.testnetPrefix = Preconditions.checkNotNull(testnetPrefix, ErrorMessages.TESTNET_PREFIX_NULL);
	}

	public String getMainnetPrefix() {
		return mainnetPrefix;
	}

	public void setMainnetPrefix(String mainnetPrefix) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.mainnetPrefix = Preconditions.checkNotNull(mainnetPrefix, ErrorMessages.MAINNET_PREFIX_NULL);
	}

	public String getRegtestPrefix() {
		return regtestPrefix;
	}

	public void setRegtestPrefix(String regtestPrefix) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.regtestPrefix = Preconditions.checkNotNull(regtestPrefix, ErrorMessages.REGTESTNET_PREFIX_NULL);
	}

	public DIDDocContinuation getDidDocContinuation() {
		return didDocContinuation;
	}

	public void setDidDocContinuation(DIDDocContinuation didDocContinuation) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.didDocContinuation = Preconditions.checkNotNull(didDocContinuation, ErrorMessages.DIDDOCCONTINUATION_NULL);
	}

	public DIDDocFileNameStrategy getDidDocFileNameStrategy() {
		return didDocFileNameStrategy;
	}

	public void setDidDocFileNameStrategy(DIDDocFileNameStrategy didDocFileNameStrategy) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.didDocFileNameStrategy = Preconditions.checkNotNull(didDocFileNameStrategy,
				ErrorMessages.DIDDOCCONTINUATION_FILENAME_STRATEGY_NULL);
	}

	public int getRequiredDepth() {
		return requiredDepth;
	}

	public void setRequiredDepth(int requiredDepth) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.requiredDepth = requiredDepth;
	}

	public long getTargetFundAmount() {
		return targetFundAmount;
	}

	public void setTargetFundAmount(long targetFundAmount) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.targetFundAmount = targetFundAmount;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.basePath = basePath;
	}

	public String getBaseUri() {
		return baseUri;
	}

	public void setBaseUri(String baseUri) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.baseUri = baseUri;
	}

	public int getUtxoUpkeepTarget(Chain chain) {
		switch (chain) {
		case MAINNET:
			return utxoUpkeepMainnet;
		case TESTNET:
			return utxoUpkeepTestnet;
		case REGTESTNET:
			return utxoUpkeepRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public int getUtxoUpkeepRegtest() {
		return utxoUpkeepRegtest;
	}

	public void setUtxoUpkeepRegtest(int utxoUpkeepRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.utxoUpkeepRegtest = utxoUpkeepRegtest;
	}

	public int getUtxoUpkeepTestnet() {
		return utxoUpkeepTestnet;
	}

	public void setUtxoUpkeepTestnet(int utxoUpkeepTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.utxoUpkeepTestnet = utxoUpkeepTestnet;
	}

	public int getUtxoUpkeepMainnet() {
		return utxoUpkeepMainnet;
	}

	public void setUtxoUpkeepMainnet(int utxoUpkeepMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.utxoUpkeepMainnet = utxoUpkeepMainnet;
	}

	public ECKey getWalletKey(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return walletKeyMainnet;
		case TESTNET:
			return walletKeyTestnet;
		case REGTESTNET:
			return walletKeyRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public ECKey getWalletKeyRegtest() {
		return walletKeyRegtest;
	}

	public void setWalletKeyRegtest(ECKey walletKeyRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletKeyRegtest = walletKeyRegtest;
	}

	public ECKey getWalletKeyTestnet() {
		return walletKeyTestnet;
	}

	public void setWalletKeyTestnet(ECKey walletKeyTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletKeyTestnet = walletKeyTestnet;
	}

	public ECKey getWalletKeyMainnet() {
		return walletKeyMainnet;
	}

	public void setWalletKeyMainnet(ECKey walletKeyMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletKeyMainnet = walletKeyMainnet;
	}

	public String getWalletPath(Chain chain) {
		switch (chain) {
		case MAINNET:
			return walletPathMainnet;
		case TESTNET:
			return walletPathTestnet;
		case REGTESTNET:
			return walletPathRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public String getWalletPathRegtest() {
		return walletPathRegtest;
	}

	public void setWalletPathRegtest(String walletPathRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPathRegtest = walletPathRegtest;
	}

	public String getWalletPathTestnet() {
		return walletPathTestnet;
	}

	public void setWalletPathTestnet(String walletPathTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPathTestnet = walletPathTestnet;
	}

	public String getWalletPathMainnet() {
		return walletPathMainnet;
	}

	public void setWalletPathMainnet(String walletPathMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPathMainnet = walletPathMainnet;
	}

	public String getWalletPrefix(Chain chain) {
		switch (chain) {
		case MAINNET:
			return walletPrefixMainnet;
		case TESTNET:
			return walletPrefixTestnet;
		case REGTESTNET:
			return walletPrefixRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public String getWalletPrefixRegtest() {
		return walletPrefixRegtest;
	}

	public void setWalletPrefixRegtest(String walletPrefixRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPrefixRegtest = Preconditions.checkNotNull(walletPrefixRegtest,
				ErrorMessages.REGTESTNET_WALLET_PREFIX_NULL);
	}

	public String getWalletPrefixTestnet() {
		return walletPrefixTestnet;
	}

	public void setWalletPrefixTestnet(String walletPrefixTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPrefixTestnet = Preconditions.checkNotNull(walletPrefixTestnet,
				ErrorMessages.TESTNET_WALLET_PREFIX_NULL);
	}

	public String getWalletPrefixMainnet() {
		return walletPrefixMainnet;
	}

	public void setWalletPrefixMainnet(String walletPrefixMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.walletPrefixMainnet = Preconditions.checkNotNull(walletPrefixMainnet,
				ErrorMessages.MAINNET_WALLET_PREFIX_NULL);
	}

	public List<PeerAddress> getPeers(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return mainnetPeers;
		case TESTNET:
			return testnetPeers;
		case REGTESTNET:
			return regtestPeers;
		default:
			throw new IllegalArgumentException();
		}
	}

	public List<PeerAddress> getRegtestPeers() {
		return regtestPeers;
	}

	public void setRegtestPeers(List<PeerAddress> regtestPeers) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.regtestPeers = regtestPeers;
	}

	public List<PeerAddress> getTestnetPeers() {
		return testnetPeers;
	}

	public void setTestnetPeers(List<PeerAddress> testnetPeers) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.testnetPeers = testnetPeers;
	}

	public List<PeerAddress> getMainnetPeers() {
		return mainnetPeers;
	}

	public void setMainnetPeers(List<PeerAddress> mainnetPeers) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.mainnetPeers = mainnetPeers;
	}

	public boolean isRunning(Chain chain) {
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);
		switch (chain) {
		case MAINNET:
			return runMainnet;
		case TESTNET:
			return runTestnet;
		case REGTESTNET:
			return runRegtest;
		default:
			throw new IllegalArgumentException();
		}
	}

	public boolean isRunMainnet() {
		return runMainnet;
	}

	public void setRunMainnet(boolean runMainnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.runMainnet = runMainnet;
	}

	public boolean isRunTestnet() {
		return runTestnet;
	}

	public void setRunTestnet(boolean runTestnet) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.runTestnet = runTestnet;
	}

	public boolean isRunRegtest() {
		return runRegtest;
	}

	public void setRunRegtest(boolean runRegtest) {
		Preconditions.checkState(!configured, ErrorMessages.DRIVER_CONFIGS_FINALIZED);
		this.runRegtest = runRegtest;
	}

}
