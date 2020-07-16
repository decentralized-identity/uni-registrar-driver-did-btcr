package uniregistrar.driver.did.btcr.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Simple Thread Factory implementation to make debug process easier
 */
public class BTCRThreadFactory implements ThreadFactory {

	private final String name;
	private final List<String> createdThreadNames;
	private int counter;

	BTCRThreadFactory(String name) {
		counter = 1;
		this.name = name;
		createdThreadNames = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public int getCounter() {
		return counter;
	}

	@Override
	public Thread newThread(@NonNull Runnable r) {
		Thread t = new Thread(r, name + "-Thread_" + counter);
		counter++;
		createdThreadNames.add(String.format("Created thread %d with name %s on %s %n", t.getId(), t.getName(),
				Timestamp.from(Instant.now())));
		return t;
	}

	public List<String> getCreatedThreadNames() {
		return createdThreadNames;
	}
}