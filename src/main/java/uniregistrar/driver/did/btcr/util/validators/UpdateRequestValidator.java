package uniregistrar.driver.did.btcr.util.validators;

import org.apache.commons.lang3.StringUtils;

import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.util.ParsingUtils;
import uniregistrar.request.UpdateRequest;

public final class UpdateRequestValidator {

	public static void validate(final UpdateRequest request, final DriverConfigs configs) throws ValidationException {

		if (request.getOptions() == null) {
			throw new ValidationException("Request options are null!");
		}

		if (StringUtils.isEmpty(request.getIdentifier())) {
			throw new ValidationException("Update request has no identifier");
		}

		String chain = ParsingUtils.parseChain(request.getOptions());
		ValidationCommons.validateChain(chain, configs);

	}
}
