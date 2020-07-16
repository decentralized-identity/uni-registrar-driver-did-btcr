package uniregistrar.driver.did.btcr.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.*;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;

import info.weboftrust.btctxlookup.*;
import info.weboftrust.btctxlookup.bitcoinconnection.AbstractBitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnection;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnectionException;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.util.BitcoinUtils;
import uniregistrar.driver.did.btcr.util.TransactionException;

/**
 * This class handles bitcoinj-wallet related operations and transaction
 * broadcasting.
 * 
 * @see WalletAppKit
 */
public class BitcoinJWalletAppKit extends AbstractBitcoinConnection implements BitcoinConnection {
	private static final Logger log = LogManager.getLogger(BitcoinJWalletAppKit.class);
	private final static String DEFAULT_FILE_NAME = "bitcoinj_";
	private final static String DEFAULT_PREFIX = "W";
	private static final BitcoinClientID CLIENT_ID = BitcoinClientID.BITCOINJ;
	private final NetworkParameters params;
	private final Chain chain;
	private final String path;
	private final String wPrefix;
	private final DidBtcrDriver driver;
	private volatile boolean online = false;
	private ECKey privKey;
	private Script.ScriptType preferredScriptType = Script.ScriptType.P2PKH;
	private WalletAppKit kit;
	private List<PeerAddress> peers;
	private BitcoindRPCBitcoinConnection fullNodeConnection;

	public BitcoinJWalletAppKit(DidBtcrDriver driver, Chain chain, String path, String wPrefix) {
		this(driver, chain, path, wPrefix, null);
	}

	/**
	 *
	 * @param driver  {@link DidBtcrDriver}
	 * @param chain   {@link Chain}
	 * @param path    path to store wallet files
	 * @param wPrefix name of the wallet file
	 * @param privKey private key of the wallet. Initially, {@link UTXOProducer}
	 *                relies on the funds addressed to the given key.
	 *
	 */
	public BitcoinJWalletAppKit(DidBtcrDriver driver, Chain chain, String path, String wPrefix,
			@Nullable ECKey privKey) {
		this.driver = driver;
		this.path = path != null ? path : System.getProperty("user.home") + "/" + DEFAULT_FILE_NAME;
		Preconditions.checkArgument(Files.isWritable(Paths.get(this.path)), "File path %s is not writeable!",
				this.path);
		this.chain = Preconditions.checkNotNull(chain);
		this.wPrefix = StringUtils.isEmpty(wPrefix) ? DEFAULT_PREFIX + "_" + chain.toString() : wPrefix;
		this.params = BitcoinUtils.chainToNetworkParameters(chain);
		this.privKey = privKey;
	}

	public void setPeers(List<PeerAddress> peers) {
		Preconditions.checkState(!online, "Cannot call after startup!");
		this.peers = Collections.unmodifiableList(peers);
	}

	public boolean isOnline() {
		return online;
	}

	public Wallet getWallet() {
		return kit.wallet();
	}

	public void start() {
		log.info("Wallet service for chain {} is starting...", chain::name);
		if (privKey != null) {
			log.info("Provided wallet key is: {}", privKey::toString);
		}
		log.info("Preferred script key is set to: {}", () -> preferredScriptType);
		log.info("Wallet path: {}", () -> path);
		log.info("Wallet prefix: {}", () -> wPrefix);

		this.kit = new WalletAppKit(driver.getContext(chain), preferredScriptType, KeyChainGroupStructure.DEFAULT,
				new File(path), wPrefix) {
			@Override
			protected void onSetupCompleted() {
				if (privKey != null) {
					wallet().importKey(privKey);
				}
				wallet().freshReceiveKey();
			}
		};

		if (CollectionUtils.isNotEmpty(peers)) {
			log.debug("Peer nodes are given: {}", () -> ArrayUtils.toString(peers));
			kit.setPeerNodes(peers.toArray(new PeerAddress[0])); // Set peer addresses to connect
		}

		if (chain == Chain.REGTESTNET) {
			kit.connectToLocalHost();
		} else {
			kit.peerGroup().addPeerDiscovery(new DnsDiscovery(params));
		}

		log.info("Waiting for the peer group to sync...");

		kit.setAutoSave(true);
		kit.setBlockingStartup(true);

		kit.setDownloadListener(new DownloadProgressTracker());

		kit.startAsync();

		log.debug("Wallet of the BitcoinJWalletAppKitConnection started in async thread, calling await running.");
		kit.awaitRunning();

		kit.peerGroup().setMinBroadcastConnections(1);

		online = true;

		log.debug("BitcoinJWalletAppKitConnection is ready!");
	}

	public Script.ScriptType getPreferredScriptType() {
		return preferredScriptType;
	}

	public void setPreferredScriptType(Script.ScriptType preferredScriptType) {
		this.preferredScriptType = preferredScriptType;
	}

	@Override
	public ChainAndTxid lookupChainAndTxid(ChainAndLocationData chainAndLocationData) {
		Preconditions.checkNotNull(fullNodeConnection, "Cannot process without full node connection!");
		return fullNodeConnection.lookupChainAndTxid(chainAndLocationData);
	}

	@Override
	public ChainAndLocationData lookupChainAndLocationData(ChainAndTxid chainAndTxid) {
		Preconditions.checkNotNull(fullNodeConnection, "Cannot process without full node connection!");
		return fullNodeConnection.lookupChainAndLocationData(chainAndTxid);
	}

	@Override
	public DidBtcrData getDidBtcrData(ChainAndTxid chainAndTxid) throws IOException {
		Preconditions.checkNotNull(fullNodeConnection, "Cannot process without full node connection!");
		return fullNodeConnection.getDidBtcrData(chainAndTxid);
	}

	@Nullable
	public String getRawTransaction(String txid) throws BitcoinConnectionException {
		Transaction tx = kit.wallet().getTransaction(Sha256Hash.wrap(txid));
		if (tx != null) {
			return new String(tx.unsafeBitcoinSerialize(), StandardCharsets.UTF_8);
		}

		if (fullNodeConnection != null) {
			return fullNodeConnection.getRawTransactionHex(txid);
		}

		return null;
	}

	public int getBlockCount() {
		return kit.peerGroup().getMostCommonChainHeight();
	}

	public Transaction sendRawTransaction(String rawtxhex) throws TransactionException {
		Transaction toBroadcast = BitcoinUtils.fromHexToBitcoinJTX(rawtxhex, chain);
		sendCompletedTransaction(toBroadcast);
		return sendCompletedTransaction(toBroadcast);
	}

	public Transaction sendCompletedTransaction(Transaction tx) throws TransactionException {
		Context.propagate(driver.getContext(chain));
		log.debug("TX send request is received for: {}", tx::getTxId);

		try {
			tx.verify();
		} catch (VerificationException e) {
			log.error("Transaction is not valid: {} ", e::getMessage);
			throw new TransactionException(e);
		}

		log.debug("Transaction broadcast is prepared.");
		kit.peerGroup().broadcastTransaction(tx);
		log.info("TX is announced: {}", tx::getTxId);
		return tx;
	}

	public boolean completeAndSendTransaction(Transaction tx) throws InsufficientMoneyException {
		Context.propagate(driver.getContext(chain));
		log.debug("TX send request is received for: {}", tx::getTxId);

		SendRequest sr = SendRequest.forTx(tx);
		sr.ensureMinRequiredFee = true;
		sr.shuffleOutputs = false;
		sr.signInputs = true;

		log.debug("Wallet balance is {}", () -> kit.wallet().getBalance().toFriendlyString());
		kit.wallet().completeTx(sr); // Funds the transaction from our UTXO set
		boolean verified = kit.wallet().maybeCommitTx(sr.tx); // Mark related UTXOs as spent
		if (verified) {
			kit.peerGroup().broadcastTransaction(sr.tx);
			log.info("Wallet TX is announced: {}", tx::getTxId);
		}

		return verified;
	}

	public SendRequest signUtxoWalletTxThenSend(Transaction tx) throws TransactionException {
		Context.propagate(driver.getContext(chain));
		Wallet wallet = driver.getUtxoWallet(chain);

		SendRequest sr = SendRequest.forTx(tx);
		sr.changeAddress = null;
		sr.signInputs = true;
		sr.ensureMinRequiredFee = false;
		sr.shuffleOutputs = false;

		wallet.signTransaction(sr);
		try {
			wallet.commitTx(sr.tx);
		} catch (VerificationException e) {
			throw new TransactionException(e);
		}
		kit.peerGroup().broadcastTransaction(sr.tx);
		log.info("TX is signed and announced: {}", sr.tx::getTxId);
		return sr;
	}

	public ListenableFuture<Transaction> sendBitcoinJTransaction(Transaction toBroadcast) {
		TransactionBroadcast broadcast = kit.peerGroup().broadcastTransaction(toBroadcast);
		return broadcast.future();
	}

	public void addWallet(Wallet wallet) {
		kit.chain().addWallet(wallet);
		kit.peerGroup().addWallet(wallet);
	}

	public Wallet creatOrLoadWallet(String wPath, boolean replayWallet) throws Exception {
		Preconditions.checkNotNull(wPath);
		return creatOrLoadWallet(new File(wPath), replayWallet);
	}

	public Wallet creatOrLoadWallet(File wFile, boolean replayWallet) throws Exception {
		Preconditions.checkArgument(wFile.canWrite());
		Wallet nWallet;
		if (!wFile.exists()) {
			nWallet = Wallet.createBasic(params);
			nWallet.saveToFile(wFile);
		}
		nWallet = loadWallet(wFile, replayWallet);
		nWallet.autosaveToFile(wFile, 3, TimeUnit.SECONDS, null);

		return nWallet;
	}

	private Wallet loadWallet(File wFile, boolean replayWallet) throws Exception {
		Wallet wallet;
		try (FileInputStream walletStream = new FileInputStream(wFile)) {
			Protos.Wallet proto = WalletProtobufSerializer.parseToProto(walletStream);
			final WalletProtobufSerializer serializer;
			serializer = new WalletProtobufSerializer();
			wallet = serializer.readWallet(params, null, proto);
			if (replayWallet)
				wallet.reset();
		}
		return wallet;
	}

	public boolean importKey(ECKey key) {
		return kit.wallet().importKey(key);
	}

	public long getBlockTimeSeconds() {
		return kit.chain().getChainHead().getHeader().getTimeSeconds();
	}

	public void stop() {
		log.info("WalletService is closing...");
		online = false;
		kit.stopAsync();
		kit.awaitTerminated();
		log.info("Wallet service is terminated!");
	}
}
