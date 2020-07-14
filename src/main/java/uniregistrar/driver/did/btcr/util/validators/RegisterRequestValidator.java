package uniregistrar.driver.did.btcr.util.validators;

import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.util.ParsingUtils;
import uniregistrar.request.RegisterRequest;

public final class RegisterRequestValidator {

	public static void validate(final RegisterRequest request, final DriverConfigs configs) throws ValidationException {
		if (request.getOptions() == null) {
			throw new ValidationException("Request options are null!");
		}
		String chain = ParsingUtils.parseChain(request.getOptions());
		ValidationCommons.validateChain(chain, configs);

	}
}
