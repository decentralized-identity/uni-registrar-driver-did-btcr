package uniregistrar.driver.did.btcr.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uniregistrar.driver.did.btcr.DriverConstants;

/**
 * Handles driver wide executor requests
 */
public final class ExecutorProvider {

	private static final Logger log = LogManager.getLogger(ExecutorProvider.class);
	private static final Map<BTCRThreadFactory, ExecutorService> executors = new HashMap<>();
	private static final Map<BTCRThreadFactory, ScheduledExecutorService> scheduledExecutors = new HashMap<>();
	private static int counter = 1;

	private ExecutorProvider() {
	}

	public static ExecutorService getFixedThreadPool(String name, int capacity) {
		log.debug("New fixed-thread-pool request with name {} and capacity {}", name + "_" + counter, capacity);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ExecutorService toReturn = Executors.newFixedThreadPool(capacity, factory);
		executors.put(factory, toReturn);

		return toReturn;
	}

	public static ExecutorService getWorkStealingPool(String name) {
		log.debug("New work-stealing-thread-pool request with name {}", name + "_" + counter);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ExecutorService toReturn = Executors.newWorkStealingPool();
		executors.put(factory, toReturn);

		return toReturn;
	}

	public static ExecutorService getWorkStealingPool(String name, int parallelism) {
		log.debug("New work-stealing-thread-pool request with name {} and parallelism of {}", name + "_" + counter,
				parallelism);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ExecutorService toReturn = Executors.newWorkStealingPool(parallelism);
		executors.put(factory, toReturn);

		return toReturn;
	}

	public static ExecutorService getCachedThreadPool(String name) {
		log.debug("New cached-thread-pool request with name {}", name + "_" + counter);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ExecutorService toReturn = Executors.newCachedThreadPool(factory);
		executors.put(factory, toReturn);

		return toReturn;
	}

	public static ExecutorService getSingleThreadExecutor(String name) {
		log.debug("New single-thread-executor request with name {} ", name + "_" + counter);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ScheduledExecutorService toReturn = Executors.newSingleThreadScheduledExecutor(factory);
		scheduledExecutors.put(factory, toReturn);

		return toReturn;
	}

	public static ScheduledExecutorService getScheduledThreadPool(String name, int corePoolSize) {
		log.debug("New scheduled-thread-pool request with name {} and core pool size of {}", name + "_" + counter,
				corePoolSize);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ScheduledExecutorService toReturn = Executors.newScheduledThreadPool(corePoolSize, factory);
		scheduledExecutors.put(factory, toReturn);

		return toReturn;
	}

	public static ScheduledExecutorService getSingleThreadScheduledExecutor(String name) {
		log.debug("New single-scheduled-executor-service  request with name {} ", name + "_" + counter);

		final BTCRThreadFactory factory = new BTCRThreadFactory(name + "_" + counter++);
		final ScheduledExecutorService toReturn = Executors.newSingleThreadScheduledExecutor(factory);
		scheduledExecutors.put(factory, toReturn);

		return toReturn;
	}

	public static void shutDown() {
		shutDown(DriverConstants.SHUTDOWN_WAITING_TIME);
	}

	public static void shutDown(int waitSec) {
		log.info("Shutting down the Executor Provider...");

		executors.values().forEach((e) -> {
			log.trace("Shutting down executor: {}", e::toString);
			e.shutdown();
		});

		scheduledExecutors.values().forEach((e) -> {
			log.trace("Shutting down scheduled executor: {}", e::toString);
			e.shutdown();
		});

		try {
			log.info("Waiting for {} seconds executors to terminate...", waitSec);
			TimeUnit.SECONDS.sleep(waitSec);
		} catch (InterruptedException e) {
			log.error("Interrupted during shut down process with exception {}", e.getMessage());
		}

		executors.forEach((t, e) -> {
			if (!e.isTerminated()) {
				e.shutdownNow();
				log.warn("BTCR Driver executors did not terminate in {} seconds...", waitSec);
				log.debug("Executor service with thread_pool name {} is abruptly shut down.", t::getName);
			}
		});

		scheduledExecutors.forEach((t, e) -> {
			if (!e.isTerminated()) {
				e.shutdownNow();
				log.warn("BTCR Driver scheduled executors did not terminate in {} seconds...", waitSec);
				log.debug("Executor service with thread_pool name {} is abruptly shut down.", t::getName);
			}
		});
	}
}
