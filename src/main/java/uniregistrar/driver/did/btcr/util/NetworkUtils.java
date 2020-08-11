package uniregistrar.driver.did.btcr.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import info.weboftrust.btctxlookup.BitcoinClientID;
import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.ChainAndLocationData;
import info.weboftrust.btctxlookup.bitcoinconnection.BTCDRPCBitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import uniregistrar.driver.did.btcr.DriverConfigs;

public final class NetworkUtils {

	private static final Logger log = LogManager.getLogger(NetworkUtils.class);

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

	public static SSLSocketFactory getSslSocketFactory(String certString) {
		try {

			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			Certificate certificate;
			try (InputStream inputStream = new ByteArrayInputStream(certString.getBytes())) {
				certificate = certificateFactory.generateCertificate(inputStream);
			}
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca-cert", certificate);

			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
			trustManagerFactory.init(keyStore);

			SSLContext context = SSLContext.getInstance("SSL");
			context.init(null, trustManagerFactory.getTrustManagers(), null);
			return context.getSocketFactory();

		} catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException
				| IOException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static ChainAndLocationData getChainAndLocationData(Chain chain, String txid, BitcoinConnection conn)
			throws IOException {
		return conn.lookupChainAndLocationData(chain, txid);
	}
}