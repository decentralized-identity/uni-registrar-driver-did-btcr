package uniregistrar.driver.did.btcr.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;

import com.google.common.net.InetAddresses;

import info.weboftrust.btctxlookup.BitcoinClientID;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.diddoccontinuation.LocalFileDIDDocContinuation;
import uniregistrar.driver.did.btcr.util.validators.ConfigurationException;

/**
 * The class where driver is prepared with provided parameters. Environment
 * variables have priority over properties.
 */
public final class Configurator {

	private static final Logger log = LogManager.getLogger(Configurator.class);

	private Configurator() {
	}

	/**
	 * @param properties Properties to use. In case it is null, default properties
	 *                   file is used to load properties
	 * @return DriverProps with settings
	 * @throws ConfigurationException Indicator for configuration related errors
	 * @implNote Prefer environment variables over properties
	 */
	public static DriverConfigs getDriverProps(@Nullable Properties properties) throws ConfigurationException {

		DriverConfigs driverConfigs = new DriverConfigs();
		Properties props = properties == null ? new Properties() : properties;

		if (props.isEmpty()) {
			log.info("No properties given, looking for the default properties file...");

			try (FileInputStream fio = new FileInputStream(
					Configurator.class.getResource(DriverConstants.PROPERTIES_FILE).getFile())) {
				props.load(fio);
			} catch (IOException e) {
				log.error(e.getMessage());
				throw new ConfigurationException(ErrorMessages.CANNOT_LOCATE_PROPERTIES);
			}
		}

		// Set active channels
		driverConfigs.setRunMainnet(Boolean.parseBoolean(System.getenv("uniregistrar_driver_did_btcr_Mainnet"))
				|| (Boolean.parseBoolean(props.getProperty("chain.mainnet"))));

		log.info("Mainnet is set to active: " + driverConfigs.isRunMainnet());

		driverConfigs.setRunTestnet(Boolean.parseBoolean(System.getenv("uniregistrar_driver_did_btcr_Testnet"))
				|| (Boolean.parseBoolean(props.getProperty("chain.testnet"))));

		log.info("Testnet is set to active: " + driverConfigs.isRunTestnet());

		driverConfigs.setRunRegtest(Boolean.parseBoolean(System.getenv("uniregistrar_driver_did_btcr_Regtest"))
				|| (Boolean.parseBoolean(props.getProperty("chain.regtest"))));

		log.info("Regtest is set to active: {}", driverConfigs.isRunRegtest());

		boolean mainnet = driverConfigs.isRunMainnet();
		boolean testnet = driverConfigs.isRunTestnet();
		boolean regtest = driverConfigs.isRunRegtest();

		// Set method related prefixes
		driverConfigs.setMethodPrefix(StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_methodPrefix"))
				? System.getenv("uniregistrar_driver_did_btcr_methodPrefix")
				: (StringUtils.isNotEmpty(props.getProperty("btcr.method.prefix"))
						? props.getProperty("btcr.method.prefix")
						: DriverConstants.METHOD_PREFIX));
		log.info("BTCR Method prefix is set to: {}", driverConfigs::getMethodPrefix);

		if (mainnet) {
			driverConfigs.setMainnetPrefix(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_mainnetPrefix"))
							? System.getenv("uniregistrar_driver_did_btcr_mainnetPrefix")
							: (StringUtils.isNotEmpty(props.getProperty("btcr.mainnet.prefix"))
									? props.getProperty("btcr.mainnet.prefix")
									: DriverConstants.MAINNET_PREFIX));

			log.info("BTCR Mainnet prefix is set to: {}", driverConfigs::getMainnetPrefix);
		}

		driverConfigs
				.setTestnetPrefix(StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_testnetPrefix"))
						? System.getenv("uniregistrar_driver_did_btcr_testnetPrefix")
						: (StringUtils.isNotEmpty(props.getProperty("btcr.testnet.prefix"))
								? props.getProperty("btcr.testnet.prefix")
								: DriverConstants.TESTNET_PREFIX));

		log.info("BTCR Testnet prefix is set to: {}", driverConfigs::getTestnetPrefix);

		if (regtest) {
			driverConfigs.setRegtestPrefix(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_regtestPrefix"))
							? System.getenv("uniregistrar_driver_did_btcr_regtestPrefix")
							: (StringUtils.isNotEmpty(props.getProperty("btcr.regtest.prefix"))
									? props.getProperty("btcr.regtest.prefix")
									: DriverConstants.REGTEST_PREFIX));

			log.info("BTCR Regtest prefix is set to: {}", driverConfigs::getRegtestPrefix);
		}

		// Set confirmation check intervals

		if (mainnet) {
			driverConfigs.setMainnetCheckInterval(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_checkIntervalMainnet"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_checkIntervalMainnet"))
							: (NumberUtils.isParsable(props.getProperty("mainnet.conf.check.interval"))
									? Integer.parseInt(props.getProperty("mainnet.conf.check.interval"))
									: DriverConstants.CHECK_INTERVAL_MAINNET));
			log.info("Mainnet check interval is set to: {}", driverConfigs::getMainnetCheckInterval);
		}

		if (testnet) {
			driverConfigs.setTestnetCheckInterval(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_checkIntervalTestnet"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_checkIntervalTestnet"))
							: (NumberUtils.isParsable(props.getProperty("testnet.conf.check.interval"))
									? Integer.parseInt(props.getProperty("testnet.conf.check.interval"))
									: DriverConstants.CHECK_INTERVAL_TESTNET));
			log.info("Testnet check interval is set to: {}", driverConfigs::getTestnetCheckInterval);
		}

		if (regtest) {
			driverConfigs.setRegtestCheckInterval(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_checkIntervalRegtest"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_checkIntervalRegtest"))
							: (NumberUtils.isParsable(props.getProperty("regtest.conf.check.interval"))
									? Integer.parseInt(props.getProperty("regtest.conf.check.interval"))
									: DriverConstants.CHECK_INTERVAL_REGTEST));
			log.info("Regtestnet check interval is set to: {}", driverConfigs::getRegtestCheckInterval);
		}

		// Set UTXO upkeep targets

		if (mainnet) {
			driverConfigs.setUtxoUpkeepMainnet(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepMainnet"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepMainnet"))
							: (NumberUtils.isParsable(props.getProperty("wallet.utxo.upkeep.mainnet"))
									? Integer.parseInt(props.getProperty("wallet.utxo.upkeep.mainnet"))
									: DriverConstants.UTXO_UPKEEP_MAINNET));
			log.info("Mainnet UTXO upkeep is set to: {}", driverConfigs::getUtxoUpkeepMainnet);
		}

		if (testnet) {
			driverConfigs.setUtxoUpkeepTestnet(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepTestnet"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepTestnet"))
							: (NumberUtils.isParsable(props.getProperty("wallet.utxo.upkeep.testnet"))
									? Integer.parseInt(props.getProperty("wallet.utxo.upkeep.testnet"))
									: DriverConstants.UTXO_UPKEEP_TESTNET));
			log.info("Testnet UTXO upkeep is set to: {}", driverConfigs::getUtxoUpkeepTestnet);
		}

		if (regtest) {
			driverConfigs.setUtxoUpkeepRegtest(
					NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepRegtest"))
							? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_utxoUpkeepRegtest"))
							: (NumberUtils.isParsable(props.getProperty("wallet.utxo.upkeep.regtest"))
									? Integer.parseInt(props.getProperty("wallet.utxo.upkeep.regtest"))
									: DriverConstants.UTXO_UPKEEP_REGTEST));
			log.info("Regtestnet UTXO upkeep is set to: {}", driverConfigs::getUtxoUpkeepRegtest);
		}

		// Set base path and base uri

		driverConfigs.setBasePath(StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_basePath"))
				? System.getenv("uniregistrar_driver_did_btcr_basePath")
				: (StringUtils.isNotEmpty(props.getProperty("base.path")) ? props.getProperty("base.path") : null));

		if (driverConfigs.getBasePath() == null) {
			throw new ConfigurationException(ErrorMessages.BASE_PATH_NULL);
		} else {
			log.info("Base path is set to: {}", driverConfigs::getBasePath);
		}

		driverConfigs.setBaseUri(StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_baseUri"))
				? System.getenv("uniregistrar_driver_did_btcr_baseUri")
				: (StringUtils.isNotEmpty(props.getProperty("base.uri")) ? props.getProperty("base.uri") : null));

		if (driverConfigs.getBaseUri() == null) {
			throw new ConfigurationException(ErrorMessages.BASE_URI_NULL);
		} else {
			log.info("Base uri is set to: {}", driverConfigs::getBaseUri);
		}

		// Set private key for wallets
		if (mainnet) {
			final String mainnetKey = StringUtils
					.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_privateKeyMainnet"))
							? System.getenv("uniregistrar_driver_did_btcr_privateKeyMainnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.mainnet.key"))
									? props.getProperty("wallet.mainnet.key")
									: null);

			if (mainnetKey == null) {
				log.info("No private key provided for mainnet wallet, creating a new one...");
				ECKey mKey = new ECKey();
				driverConfigs.setWalletKeyMainnet(mKey);
			} else {
				log.debug("Mainnet private key is: {}", () -> mainnetKey);
				ECKey mecKey = DumpedPrivateKey.fromBase58(MainNetParams.get(), mainnetKey).getKey();
				driverConfigs.setWalletKeyMainnet(mecKey);
			}
		}

		if (testnet) {
			final String testnetKey = StringUtils
					.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_privateKeyTestnet"))
							? System.getenv("uniregistrar_driver_did_btcr_privateKeyTestnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.testnet.key"))
									? props.getProperty("wallet.testnet.key")
									: null);

			if (testnetKey == null) {
				log.info("No private key provided for testnet wallet, creating a new one...");
				ECKey tKey = new ECKey();
				driverConfigs.setWalletKeyTestnet(tKey);
			} else {
				log.debug("Testnet private key is: {}", () -> testnetKey);
				ECKey tecKey = DumpedPrivateKey.fromBase58(TestNet3Params.get(), testnetKey).getKey();
				driverConfigs.setWalletKeyTestnet(tecKey);
			}
		}

		if (regtest) {
			final String regtestKey = StringUtils
					.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_privateKeyRegtest"))
							? System.getenv("uniregistrar_driver_did_btcr_privateKeyRegtest")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.regtest.key"))
									? props.getProperty("wallet.regtest.key")
									: null);

			if (regtestKey == null) {
				log.info("No private key provided for testnet wallet, creating a new one...");
				ECKey rKey = new ECKey();
				driverConfigs.setWalletKeyRegtest(rKey);
			} else {
				log.debug("Regtest private key is: {}", () -> regtestKey);
				ECKey tecKey = DumpedPrivateKey.fromBase58(RegTestParams.get(), regtestKey).getKey();
				driverConfigs.setWalletKeyRegtest(tecKey);
			}
		}

		// Set wallet paths

		if (mainnet) {
			driverConfigs.setWalletPathMainnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPathMainnet"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPathMainnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.path.mainnet"))
									? props.getProperty("wallet.path.mainnet")
									: DriverConstants.WALLET_PATH));

			log.info("Mainnet wallet path is set to: {}", driverConfigs::getWalletPathMainnet);
		}

		if (testnet) {
			driverConfigs.setWalletPathTestnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPathTestnet"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPathTestnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.path.testnet"))
									? props.getProperty("wallet.path.testnet")
									: DriverConstants.WALLET_PATH));

			log.info("Testnet wallet path is set to: {}", driverConfigs::getWalletPathTestnet);
		}

		if (regtest) {
			driverConfigs.setWalletPathRegtest(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPathRegtest"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPathRegtest")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.path.regtest"))
									? props.getProperty("wallet.path.regtest")
									: DriverConstants.WALLET_PATH));

			log.info("Regtestnet wallet path is set to: {}", driverConfigs::getWalletPathRegtest);
		}

		// Set wallet prefixes

		if (mainnet) {
			driverConfigs.setWalletPrefixMainnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPrefixMainnet"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPrefixMainnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.prefix.mainnet"))
									? props.getProperty("wallet.prefix.mainnet")
									: DriverConstants.WALLET_PREFIX_MAINNET));

			log.info("Mainnet wallet prefix is set to: {}", driverConfigs::getWalletPrefixMainnet);
		}

		if (testnet) {
			driverConfigs.setWalletPrefixTestnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPrefixTestnet"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPrefixTestnet")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.prefix.testnet"))
									? props.getProperty("wallet.prefix.testnet")
									: DriverConstants.WALLET_PREFIX_TESTNET));

			log.info("Testnet wallet prefix is set to: {}", driverConfigs::getWalletPrefixTestnet);
		}

		if (regtest) {
			driverConfigs.setWalletPrefixRegtest(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletPrefixRegtest"))
							? System.getenv("uniregistrar_driver_did_btcr_walletPrefixRegtest")
							: (StringUtils.isNotEmpty(props.getProperty("wallet.prefix.regtest"))
									? props.getProperty("wallet.prefix.regtest")
									: DriverConstants.WALLET_PREFIX_REGTEST));

			log.info("Regtestnet wallet prefix is set to: {}", driverConfigs::getWalletPathRegtest);
		}

		// Set required depth for tx confirmations

		driverConfigs
				.setRequiredDepth(NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_requiredTxDepth"))
						? Integer.parseInt(System.getenv("uniregistrar_driver_did_btcr_requiredTxDepth"))
						: (NumberUtils.isParsable(props.getProperty("btcr.required.tx.depth"))
								? Integer.parseInt(props.getProperty("btcr.required.tx.depth"))
								: DriverConstants.REQUIRED_TX_DEPTH));
		log.info("Required TX depth is set to: {}", driverConfigs::getRequiredDepth);

		// Set target fund amount to prepare UTXOs

		driverConfigs.setTargetFundAmount(
				NumberUtils.isParsable(System.getenv("uniregistrar_driver_did_btcr_targetFundAmount"))
						? Long.parseLong(System.getenv("uniregistrar_driver_did_btcr_targetFundAmount"))
						: (NumberUtils.isParsable(props.getProperty("btcr.target.fund.amount"))
								? Long.parseLong(props.getProperty("btcr.target.fund.amount"))
								: DriverConstants.TARGET_FUND_AMOUNT));
		log.info("Target fund amount is set to: {}", driverConfigs::getTargetFundAmount);

		// Set RPC URLs

		if (mainnet) {
			driverConfigs.setRpcUrlMainnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_rpcUrlMainnet"))
							? System.getenv("uniregistrar_driver_did_btcr_rpcUrlMainnet")
							: (StringUtils.isNotEmpty(props.getProperty("conn.rpc.url.mainnet"))
									? props.getProperty("conn.rpc.url.mainnet")
									: null));

			if (driverConfigs.getRpcUrlMainnet() == null) {
				throw new ConfigurationException(ErrorMessages.MAINNET_RPC_NULL);
			} else {
				log.info("Mainnet rpc url is set to: {}", driverConfigs::getRpcUrlMainnet);
			}
		}

		if (testnet) {
			driverConfigs.setRpcUrlTestnet(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_rpcUrlTestnet"))
							? System.getenv("uniregistrar_driver_did_btcr_rpcUrlTestnet")
							: (StringUtils.isNotEmpty(props.getProperty("conn.rpc.url.testnet"))
									? props.getProperty("conn.rpc.url.testnet")
									: null));

			if (driverConfigs.getRpcUrlTestnet() == null) {
				throw new ConfigurationException(ErrorMessages.TESTNET_RPC_NULL);
			} else {
				log.info("Testnet rpc url is set to: {}", driverConfigs::getRpcUrlTestnet);
			}
		}

		if (regtest) {
			driverConfigs.setRpcUrlRegtest(
					StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_rpcUrlRegtest"))
							? System.getenv("uniregistrar_driver_did_btcr_rpcUrlRegtest")
							: (StringUtils.isNotEmpty(props.getProperty("conn.rpc.url.regtest"))
									? props.getProperty("conn.rpc.url.regtest")
									: null));

			if (driverConfigs.getRpcUrlRegtest() == null) {
				throw new ConfigurationException(ErrorMessages.REGTESTNET_RPC_NULL);
			} else {
				log.info("Regtestnet rpc url is set to: {}", driverConfigs::getRpcUrlRegtest);
			}
		}

		if (mainnet) {
			driverConfigs.setMainnetPeers(getPeers(props, "conn.mainnet.peer.", DriverConstants.MAINNET_PARAMS));
		}

		if (testnet) {
			driverConfigs.setTestnetPeers(getPeers(props, "conn.testnet.peer.", DriverConstants.TESTNET_PARAMS));
		}

		if (regtest) {
			driverConfigs.setRegtestPeers(getPeers(props, "conn.regtest.peer.", DriverConstants.REGTEST_PARAMS));
		}

		final String envStrOvr = System.getenv("uniregistrar_driver_did_btcr_overWriteDidDocs");
		final boolean envBoolOvr = StringUtils.isNotEmpty(envStrOvr);

		driverConfigs.setOverWriteDidDocs(envBoolOvr ? Boolean.parseBoolean(envStrOvr)
				: (Boolean.parseBoolean(props.getProperty("btcr.did.doc.continuation.overWriteDidDocs"))));

		log.info("DID Continuation documents will be overwritten on updates: {}", driverConfigs.isOverWriteDidDocs());

		if (mainnet) {
			final String envStr = System.getenv("uniregistrar_driver_did_btcr_utxoProducingMainnet");
			final boolean envBool = StringUtils.isNotEmpty(envStr);
			driverConfigs.setActivateUTXOProducingMainnet(envBool ? Boolean.parseBoolean(envStr)
					: (Boolean.parseBoolean(props.getProperty("wallet.produce.utxo.mainnet"))));

			log.info("UTXO Producing for mainnet is set to: {}", driverConfigs::isActivateUTXOProducingMainnet);
		}

		if (testnet) {
			final String envStr = System.getenv("uniregistrar_driver_did_btcr_utxoProducingTestnet");
			final boolean envBool = StringUtils.isNotEmpty(envStr);
			driverConfigs.setActivateUTXOProducingTestnet(envBool ? Boolean.parseBoolean(envStr)
					: (Boolean.parseBoolean(props.getProperty("wallet.produce.utxo.testnet"))));

			log.info("UTXO Producing for testnet is set to: {}", driverConfigs::isActivateUTXOProducingTestnet);
		}

		if (regtest) {
			final String envStr = System.getenv("uniregistrar_driver_did_btcr_utxoProducingRegtest");
			final boolean envBool = StringUtils.isNotEmpty(envStr);
			driverConfigs.setActivateUTXOProducingRegtest(envBool ? Boolean.parseBoolean(envStr)
					: (Boolean.parseBoolean(props.getProperty("wallet.produce.utxo.regtest"))));

			log.info("UTXO Producing for regtestnet is set to: {}", driverConfigs::isActivateUTXOProducingRegtest);
		}

		// Setting defaults that will parameterized later
		driverConfigs.setDidDocContinuation(
				new LocalFileDIDDocContinuation(driverConfigs.getBasePath(), driverConfigs.getBaseUri()));

		// Set preferred script type
		String scriptType = StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_walletScriptType"))
				? System.getenv("uniregistrar_driver_did_btcr_walletScriptType")
				: (StringUtils.isNotEmpty(props.getProperty("wallet.script.type"))
						? props.getProperty("wallet.script.type")
						: null);

		if (scriptType != null && scriptType.equalsIgnoreCase("P2WPKH")) {
			driverConfigs.setPrefScriptType(Script.ScriptType.P2WPKH);
		} else {
			driverConfigs.setPrefScriptType(DriverConstants.SCRIPT_TYPE);
		}
		log.info("Preferred script-type is set to: {}", driverConfigs.getPrefScriptType()::toString);

		String client = StringUtils.isNotEmpty(System.getenv("uniregistrar_driver_did_btcr_bitcoinConnection"))
				? System.getenv("uniregistrar_driver_did_btcr_bitcoinConnection")
				: (StringUtils.isNotEmpty(props.getProperty("bitcoin.client")) ? props.getProperty("bitcoin.client")
						: DriverConstants.BITCOIN_CLIENT.name());

		// Set bitcoin client
		driverConfigs.setBitcoinClientID(BitcoinClientID.fromString(client));
		log.info("Bitcoin client is set to {}", driverConfigs::getBitcoinClientID);

		// Follow deactivation transactions or directly give the result?
		driverConfigs.setFollowDeactivate(
				Boolean.parseBoolean(System.getenv("uniregistrar_driver_did_btcr_followDeactivation"))
						|| (Boolean.parseBoolean(props.getProperty("driver.follow.deactivation"))));

		log.info("Following deactivation transactions is set to: {}", driverConfigs.isFollowDeactivate());

		return driverConfigs;
	}

	private static List<PeerAddress> getPeers(Properties props, String keyFor, NetworkParameters params)
			throws ConfigurationException {

		// Hot fix taking env. variables as priority
		if (params == DriverConstants.MAINNET_PARAMS) {
			String url = System.getenv("uniregistrar_driver_did_btcr_peerMainnet");
			if (StringUtils.isNotEmpty(url)) {
				log.info("Mainnet Peer given: {}", url);
				String[] temp = url.split(":");
				if (temp.length != 2) {
					return null;
				}
				InetAddress inetAddress;

				try {
					inetAddress = InetAddress.getByName(temp[0]);
				} catch (UnknownHostException e) {
					log.error(e.getMessage());
					throw new ConfigurationException("Invalid Peer address for mainnet");
				}

				PeerAddress peer = new PeerAddress(params, inetAddress, NumberUtils.toInt(temp[1]));
				return Collections.singletonList(peer);
			}
		}

		if (params == DriverConstants.TESTNET_PARAMS) {
			String url = System.getenv("uniregistrar_driver_did_btcr_peerTestnet");
			if (StringUtils.isNotEmpty(url)) {
				log.info("Testnet Peer given: {}", url);
				String[] temp = url.split(":");
				if (temp.length != 2) {
					return null;
				}
				InetAddress inetAddress;

				try {
					inetAddress = InetAddress.getByName(temp[0]);
				} catch (UnknownHostException e) {
					log.error(e.getMessage());
					throw new ConfigurationException("Invalid Peer address for testnet");
				}

				PeerAddress peer = new PeerAddress(params, inetAddress, NumberUtils.toInt(temp[1]));
				return Collections.singletonList(peer);
			}
		}

		Enumeration<Object> eKeys = props.keys();
		List<PeerAddress> peers = new ArrayList<>();

		while (eKeys.hasMoreElements()) {
			String key = (String) eKeys.nextElement();
			if (key != null && key.contains(keyFor)) {

				String[] temp = props.getProperty(key).split(":");
				if (temp.length != 2) {
					continue;
				}

				if (temp[0].equalsIgnoreCase("localhost")) {
					try {
						final InetAddress localHost = InetAddress.getLocalHost();
						PeerAddress peer = new PeerAddress(params, localHost, NumberUtils.toInt(temp[1]));
						peers.add(peer);
					} catch (UnknownHostException e) {
						log.error("Cannot set localhost peers", e);
					}
				} else {
					PeerAddress peer = new PeerAddress(params, InetAddresses.forString(temp[0]),
							NumberUtils.toInt(temp[1]));
					peers.add(peer);
				}
			}
		}
		return peers;
	}

}
