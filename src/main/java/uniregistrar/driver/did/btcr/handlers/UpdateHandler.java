package uniregistrar.driver.did.btcr.handlers;

import uniregistrar.RegistrationException;
import uniregistrar.request.UpdateRequest;
import uniregistrar.state.UpdateState;

/**
 * A simple interface to enable different implementation possibilities to handle
 * update requests.
 */
public interface UpdateHandler {
	/**
	 *
	 * @param request {@link UpdateRequest}
	 * @return {@link UpdateState} of the operation
	 * @throws RegistrationException RegistrationException
	 */
	UpdateState handle(UpdateRequest request) throws RegistrationException;
}
