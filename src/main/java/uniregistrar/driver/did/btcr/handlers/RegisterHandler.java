package uniregistrar.driver.did.btcr.handlers;

import uniregistrar.RegistrationException;
import uniregistrar.request.RegisterRequest;
import uniregistrar.state.RegisterState;

/**
 * A simple interface to enable different implementation possibilities to handle
 * register requests.
 */
public interface RegisterHandler {
	/**
	 *
	 * @param request {@link RegisterRequest}
	 * @return {@link RegisterState} of the operation
	 * @throws RegistrationException RegistrationException
	 */
	RegisterState handle(RegisterRequest request) throws RegistrationException;
}
