package uniregistrar.driver.did.btcr;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;

public final class DriverConstants {

	public static final String DID_CONTINUATION = "localfile";
	public static final String METHOD_PREFIX = "did:btcr:";
	public static final String REGTEST_PREFIX = "txtest1:";
	public static final String TESTNET_PREFIX = "txtest1:";
	public static final String MAINNET_PREFIX = "tx1:";
	public static final int CHECK_INTERVAL_REGTEST = 100;
	public static final int CHECK_INTERVAL_TESTNET = 500;
	public static final int CHECK_INTERVAL_MAINNET = 500;
	public static final int UTXO_UPKEEP_REGTEST = 300;
	public static final int UTXO_UPKEEP_TESTNET = 50;
	public static final int UTXO_UPKEEP_MAINNET = 1;
	public static final int REQUIRED_TX_DEPTH = 1;
	public static final String PROPERTIES_FILE = "/driver.properties";
	public static final String WALLET_PREFIX_MAINNET = "mainnet-wallet";
	public static final String WALLET_PREFIX_TESTNET = "testnet-wallet";
	public static final String WALLET_PREFIX_REGTEST = "regtest-wallet";
	public static final long TARGET_FUND_AMOUNT = 100000;
	public static final String WALLET_PATH = "/opt/wallets";
	public static final Script.ScriptType SCRIPT_TYPE = Script.ScriptType.P2PKH;
	public static final NetworkParameters REGTEST_PARAMS = RegTestParams.get();
	public static final NetworkParameters MAINNET_PARAMS = MainNetParams.get();
	public static final NetworkParameters TESTNET_PARAMS = TestNet3Params.get();
	public static final boolean ROTATE_KEY = false;
	// Waiting time in seconds when shutdown is triggered
	public static final int SHUTDOWN_WAITING_TIME = 3;
	public static final BitcoinClientID BITCOIN_CLIENT = BitcoinClientID.BITCOIND;
	public static final long DEFAULT_FUNDING_WAIT_TIME = 24; // In hours
	public static final Coin DEFAULT_TX_FEE_KB = Transaction.DEFAULT_TX_FEE.div(50);

	public static final Coin DEFAULT_TX_FEE = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(5); // Pay 5 times more than the min fee
	public static final Long MAX_TRACK_SECONDS = 36000L;
	public static final Chain DEFAULT_CHAIN = Chain.TESTNET;

	public static final Coin TARGET_UTXO_VALUE = Coin.MILLICOIN;
	public static final int OUTPUT_COUNT_PER_TX = 10;
	// This is a fallback number, could be removed
	public static final int TARGET_UTXO_UPKEEP = 10;

	public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
	public static final String END_CERT = "-----END CERTIFICATE-----";

	public final static String LINE_SEPARATOR = System.getProperty("line.separator");

	private DriverConstants() {
	}
}
