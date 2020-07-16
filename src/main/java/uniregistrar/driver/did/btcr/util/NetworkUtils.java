package uniregistrar.driver.did.btcr.util;

import java.io.IOException;
import java.net.MalformedURLException;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.ChainAndLocationData;
import info.weboftrust.btctxlookup.bitcoinconnection.BTCDRPCBitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import uniregistrar.driver.did.btcr.DriverConfigs;

public final class NetworkUtils {

	private NetworkUtils() {
	}

	public static BitcoindRPCBitcoinConnection getBitcoindRpcClient(DriverConfigs configs, Chain chain)
			throws MalformedURLException {
		BitcoinClientID clientID = configs.getBitcoinClientID();
		switch (chain) {
		case MAINNET:
			if (clientID == BitcoinClientID.BITCOIND) {
				return new BitcoindRPCBitcoinConnection(configs.getRpcUrlMainnet(), Chain.MAINNET);
			} else if (clientID == BitcoinClientID.BTCD) {
				return new BTCDRPCBitcoinConnection(configs.getRpcUrlMainnet(), Chain.MAINNET);
			} else
				throw new IllegalArgumentException();
		case TESTNET:
			if (clientID == BitcoinClientID.BITCOIND) {
				return new BitcoindRPCBitcoinConnection(configs.getRpcUrlTestnet(), Chain.TESTNET);
			} else if (clientID == BitcoinClientID.BTCD) {
				return new BTCDRPCBitcoinConnection(configs.getRpcUrlTestnet(), Chain.TESTNET);
			} else
				throw new IllegalArgumentException();
		case REGTESTNET:
			if (clientID == BitcoinClientID.BITCOIND) {
				return new BitcoindRPCBitcoinConnection(configs.getRpcUrlRegtest(), Chain.REGTESTNET);
			} else if (clientID == BitcoinClientID.BTCD) {
				return new BTCDRPCBitcoinConnection(configs.getRpcUrlRegtest(), Chain.REGTESTNET);
			} else
				throw new IllegalArgumentException();
		default:
			throw new IllegalArgumentException();
		}
	}

	public static ChainAndLocationData getChainAndLocationData(Chain chain, String txid, BitcoinConnection conn)
			throws IOException {
		return conn.lookupChainAndLocationData(chain, txid);
	}
}
