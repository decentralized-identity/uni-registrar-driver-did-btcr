package uniregistrar.driver.did.btcr.transaction;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.Chain;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoinConnectionException;
import info.weboftrust.btctxlookup.bitcoinconnection.BitcoindRPCBitcoinConnection;
import uniregistrar.driver.did.btcr.DidBtcrDriver;
import uniregistrar.driver.did.btcr.DidBtcrJob;
import uniregistrar.driver.did.btcr.DriverConstants;
import uniregistrar.driver.did.btcr.service.ExecutorProvider;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

/**
 * This class implemented after having issues with BitcoinJ's
 * {@link org.bitcoinj.core.TransactionConfidence} feature. Checks confirmations
 * in primitive way with full bitcoin nodes. BTCD and Bitcoind is supported.
 */
public class BitcoinConfirmationTracker {

	private static final Logger log = LogManager.getLogger(BitcoinConfirmationTracker.class);
	private static final int INITIAL_DELAY = 30;
	// <TXHash,JobID>
	private final Map<String, String> confirmationWaiters;
	// private final Map<String, Integer> countTrials;
	private final Map<String, Long> trackTime;
	private final DidBtcrDriver driver;
	private final BitcoindRPCBitcoinConnection bitcoinClient;
	private final Chain chain;
	private final int requiredDepth;

	/**
	 * 
	 * @param driver        {@link DidBtcrDriver}
	 * @param chain         {@link Chain}
	 * @param requiredDepth required depth in blocks to consider transaction as
	 *                      confirmed
	 * @param bitcoinClient {@link BitcoindRPCBitcoinConnection}
	 */
	public BitcoinConfirmationTracker(DidBtcrDriver driver, Chain chain, int requiredDepth,
			BitcoindRPCBitcoinConnection bitcoinClient) {
		this.driver = driver;
		this.chain = chain;
		this.requiredDepth = requiredDepth;
		this.bitcoinClient = bitcoinClient;
		this.confirmationWaiters = new ConcurrentHashMap<>();
		this.trackTime = new HashMap<>();
	}

	/**
	 * Starts the confirmation check process
	 * 
	 * @param watchInterval Schedules process with the given interval (in seconds)
	 */
	public void start(int watchInterval) {
		if (watchInterval <= 0) {
			log.debug("Given confirmation interval is {}, skipping scheduled confirmation checks...",
					() -> watchInterval);
			return;
		}
		log.debug("TX watcher requested with interval of {} seconds.", () -> watchInterval);
		final ScheduledExecutorService bitcoindWatcher = ExecutorProvider
				.getSingleThreadScheduledExecutor("Bitcoin_Watcher_" + chain.toString());
		bitcoindWatcher.scheduleAtFixedRate(this::checkConfirmationsWithBitcoind, INITIAL_DELAY, watchInterval,
				TimeUnit.SECONDS);
	}

	private void checkConfirmationsWithBitcoind() {
		final List<String> txList = new ArrayList<>(confirmationWaiters.keySet());
		final Map<String, CompletableFuture<Boolean>> confStates = new LinkedHashMap<>();
		for (String txId : txList) {
			CompletableFuture<Boolean> comp = CompletableFuture.supplyAsync(() -> checkConfirmationsWithBitcoin(txId));
			confStates.put(txId, comp);
		}

		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}

		for (Map.Entry<String, CompletableFuture<Boolean>> entry : confStates.entrySet()) {
			boolean confirmed = false;
			try {
				confirmed = entry.getValue().get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				log.error(e.getMessage(), e);
			}
			String txid = entry.getKey();
			if (confirmed) {
				log.debug("TX {} is confirmed via trigger of bitcoin watcher", () -> txid);
			} else {
				Long curr = Instant.now().getEpochSecond();
				long diff = curr - trackTime.get(txid);
				if (diff >= DriverConstants.MAX_TRACK_SECONDS) {
					driver.jobFailed(confirmationWaiters.remove(txid), "tx is not confirm by the network");
					trackTime.remove(txid);
				}
			}
		}
	}

	/**
	 * 
	 * @param txID Transaction ID
	 * @return True if transaction with the given id is confirmed, false otherwise
	 */
	public boolean checkConfirmationsWithBitcoin(String txID) {
		BitcoindRPCBitcoinConnection client = driver.getRpcClient(chain);
		Preconditions.checkNotNull(client, "Cannot get bitcoind client");
		log.debug("Checking confirmations for tx id {} ", () -> txID);
		BitcoindRpcClient.RawTransaction raw;

		try {
			raw = client.getRawTransaction(txID);
		} catch (Exception e) {
			log.error(e);
			return false;
		}

		Preconditions.checkNotNull(raw, "Cannot get the TX from bitcoin client");
		log.trace("RAW TX for txID {} is:\n{}", () -> txID, () -> raw);
		int confirms = 0;
		if (raw.confirmations() != null) {
			confirms = raw.confirmations();
		}
		if (confirms >= requiredDepth) {
			log.debug("Transaction has {} confirmations.", confirms);
			String jobId = confirmationWaiters.get(txID);
			log.debug("Job {} is ready for completion.", () -> jobId);
			driver.completionReady(jobId);
			try {
				confirmationWaiters.remove(txID);
			} catch (Exception e) {
				log.error(e.getMessage());
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param jobId {@link DidBtcrJob#getJobId()}
	 * @return True if the transaction corresponding ot the given job id is
	 *         confirmed, false otherwise
	 */
	public boolean checkConfirmationsWithJobID(String jobId) {
		String txID = null;
		for (Map.Entry<String, String> entry : confirmationWaiters.entrySet()) {
			if (jobId.equals(entry.getValue())) {
				txID = entry.getKey();
				break;
			}
		}

		if (txID == null) {
			log.debug("Given JobID {} has no corresponding TX", () -> jobId);
			return false;
		}

		log.debug("Checking confirmations for the jobID {} tx id of {} ", jobId, txID);
		BitcoindRpcClient.RawTransaction raw;
		try {
			raw = bitcoinClient.getRawTransaction(txID);
		} catch (GenericRpcException | BitcoinConnectionException e) {
			log.error(e);
			return false;
		}
		Preconditions.checkNotNull(raw, "Cannot get the TX from bitcoin client");
		log.trace("RAW TX for txID {} is: ", txID);
		log.trace(() -> raw);
		Integer confirms = null;
		if (raw.confirmations() != null) {
			confirms = raw.confirmations();
		}
		if (confirms != null && confirms >= requiredDepth) {
			log.debug("Transaction has {} confirmations.", confirms);
			confirmationWaiters.remove(txID);
			return true;
		}
		return false;
	}

	/**
	 * @param jobId Related jobId corresponds to the transaction
	 * @param txid  Transaction to follow which is related to the given jobId
	 */

	public void followTransaction(String jobId, String txid) {
		log.debug("New transaction follow request received.");
		log.debug("Following the job {} with tx id of: {}", jobId, txid);
		confirmationWaiters.put(txid, jobId);
		trackTime.put(txid, Instant.now().getEpochSecond());
	}
}
