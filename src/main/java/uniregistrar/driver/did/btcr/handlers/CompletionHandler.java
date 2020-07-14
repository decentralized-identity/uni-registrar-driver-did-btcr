package uniregistrar.driver.did.btcr.handlers;

import java.io.IOException;

import uniregistrar.RegistrationException;
import uniregistrar.driver.did.btcr.DidBtcrJob;

/**
 * Completion handlers are designed to be work as separate execution service.
 * Therefore, start and stop methods are made available for Thread handling.
 */
public interface CompletionHandler {
	/**
	 * Start's the completion handler.
	 */
	void start();

	/**
	 * Stops the completion handler. Gives a buffer time to clean resources.
	 */
	void stop();

	void completeUpdate(DidBtcrJob job) throws IOException, RegistrationException;

	void completeRegistration(DidBtcrJob job) throws RegistrationException, IOException;

	void completeDeactivate(DidBtcrJob job) throws IOException, RegistrationException;
}
