package uniregistrar.driver.did.btcr.handlers;

import uniregistrar.RegistrationException;
import uniregistrar.request.CreateRequest;
import uniregistrar.state.CreateState;

/**
 * A simple interface to enable different implementation possibilities to handle
 * create requests.
 */
public interface CreateHandler {
	/**
	 *
	 * @param request {@link CreateRequest}
	 * @return {@link CreateState} of the operation
	 * @throws RegistrationException RegistrationException
	 */
	CreateState handle(CreateRequest request) throws RegistrationException;
}
