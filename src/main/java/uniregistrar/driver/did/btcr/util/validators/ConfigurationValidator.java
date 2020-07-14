package uniregistrar.driver.did.btcr.util.validators;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.Script;

import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DriverConfigs;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

public class ConfigurationValidator {
	private static final Logger log = LogManager.getLogger(ConfigurationValidator.class);

	public static void preStartValidate(DriverConfigs configs) throws ConfigurationException {

		log.info("Validating the configurations...");

		if (!configs.isRunRegtest() && !configs.isRunTestnet() && !configs.isRunMainnet()) {
			throw new ConfigurationException("No chain set to active!");
		}

		if (configs.isRunRegtest() && (configs.isRunTestnet() || configs.isRunMainnet())) {
			throw new ConfigurationException("Regtestnet can be activated only when no other chain is active!");
		}

		if (configs.getRequiredDepth() < 1) {
			throw new ConfigurationException("Required depth cannot be < 1");
		}

		if (configs.getTargetFundAmount() * 2 <= Transaction.DEFAULT_TX_FEE.getValue()) {
			throw new ConfigurationException("Target fund amount is too small!");
		}

		if (configs.getPrefScriptType() != Script.ScriptType.P2PKH
				&& configs.getPrefScriptType() != Script.ScriptType.P2WPKH) {
			throw new ConfigurationException("Preferred script type must be P2PKH or P2WPKH");
		}

		if (!Files.isWritable(Paths.get(configs.getBasePath()))) {
			throw new ConfigurationException("Base path is not writeable!");
		}

		if (!Files.isDirectory(Paths.get(configs.getBasePath()))) {
			throw new ConfigurationException("Base path is not a folder!");
		}

		if (configs.isRunMainnet()) {
			if (!Files.isWritable(Paths.get(configs.getWalletPathMainnet()))) {
				throw new ConfigurationException("Wallet path (Mainnet) is not writeable!");
			}
			if (configs.isActivateUTXOProducingMainnet() && configs.getUtxoUpkeepMainnet() < 1) {
				throw new ConfigurationException("UTXO upkeep (Mainnet) cannot be < 1");
			}
			if (configs.getWalletKeyMainnet() == null) {
				throw new ConfigurationException("Mainnet wallet-key is null!");
			}
		}

		if (configs.isRunTestnet()) {
			if (!Files.isWritable(Paths.get(configs.getWalletPathTestnet()))) {
				throw new ConfigurationException("Wallet path (Testnet) is not writeable!");
			}
			if (configs.isActivateUTXOProducingTestnet() && configs.getUtxoUpkeepTestnet() < 1) {
				throw new ConfigurationException("UTXO upkeep (Testnet) cannot be < 1");
			}
			if (configs.getWalletKeyTestnet() == null) {
				throw new ConfigurationException("Testnet wallet-key is null!");
			}
		}

		if (configs.isRunRegtest()) {
			if (!Files.isWritable(Paths.get(configs.getWalletPathRegtest()))) {
				throw new ConfigurationException("Wallet path (Regtestnet) is not writeable!");
			}
			if (configs.isActivateUTXOProducingRegtest() && configs.getUtxoUpkeepRegtest() < 1) {
				throw new ConfigurationException("UTXO upkeep (Regtestnet) cannot be < 1");
			}
			if (configs.getWalletKeyRegtest() == null) {
				throw new ConfigurationException("Regtest wallet-key is null!");
			}
		}
	}

	public static void postStartValidate(DidBtcrDriver driver, DriverConfigs configs) throws ConfigurationException {

		log.info("Validating the driver setup...");

//		try {
//			log.debug("Looking for the BitcoinConnection...");
//			BitcoinConnection conn = NetworkUtils.getBitcoinConnection(driver, configs);
//			if (conn == null) {
//				throw new ConfigurationException("Bitcoin Connection is null");
//			}
//		} catch (MalformedURLException e) {
//			log.error(e.getMessage());
//			throw new ConfigurationException("BitcoinConnection URL is malformed");
//		}

		if (configs.isRunTestnet()) {
			BitcoinJSONRPCClient client = driver.getRpcClient(Chain.TESTNET).getBitcoindRpcClient();
			;
			if (client == null) {
				throw new ConfigurationException("RPC Client for the Bitcoin testnet is null!");
			}
			log.info("Testnet RPC-Client URL: {}", () -> client.rpcURL);
			try {
				BitcoindRpcClient.RawTransaction rawTx = client
						.getRawTransaction("f140d3a615bc7ce426584546e094f775d9c14ed09053f500c0e7f94a22c1e0c5");
				if (rawTx == null) {
					throw new ConfigurationException(
							"Testnet RPC client cannot get raw transactions! Check your connection and node setup!");
				}
				// TODO: Check for scantxoutset method too...
			} catch (GenericRpcException e) {
				log.info("Testnet RPC client cannot get raw transactions! Check your connection and node setup!");
			}
		}

		if (configs.isRunMainnet()) {
			BitcoinJSONRPCClient client = driver.getRpcClient(Chain.MAINNET).getBitcoindRpcClient();
			if (client == null) {
				throw new ConfigurationException("RPC Client for the Bitcoin mainnet is null!");
			}
			// TODO: Try getting raw TXs
			log.info("Mainnet RPC-Client URL: {}", () -> client.rpcURL);
		}

		if (configs.isRunRegtest()) {
			BitcoinJSONRPCClient client = driver.getRpcClient(Chain.REGTESTNET).getBitcoindRpcClient();
			if (client == null) {
				throw new ConfigurationException("RPC Client for the Bitcoin regtestnet is null!");
			}
			// TODO: Try getting raw TXs
			log.info("Regtestnet RPC-Client URL: {}", () -> client.rpcURL);
		}

		InetAddress inetAddress;
		log.info("Looking for your local IP Address...");
		try {
			inetAddress = InetAddress.getLocalHost();
			log.info("If you use local bitcoin-node:");
			log.info("Make sure your local IP address is whitelisted in your local node configuration: "
					+ inetAddress.getHostAddress());
		} catch (UnknownHostException e) {
			log.error(e.getMessage());
		}
	}
}
