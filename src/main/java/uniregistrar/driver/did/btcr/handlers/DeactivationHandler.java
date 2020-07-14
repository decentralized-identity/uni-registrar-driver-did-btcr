package uniregistrar.driver.did.btcr.handlers;

import uniregistrar.RegistrationException;
import uniregistrar.request.DeactivateRequest;
import uniregistrar.state.DeactivateState;

/**
 * A simple interface to enable different implementation possibilities to handle
 * deactivation requests.
 */
public interface DeactivationHandler {
	/**
	 * 
	 * @param request {@link DeactivateRequest}
	 * @return {@link DeactivateState} of the operation
	 * @throws RegistrationException RegistrationException
	 */
	DeactivateState handle(DeactivateRequest request) throws RegistrationException;
}
