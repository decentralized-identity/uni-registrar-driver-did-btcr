package uniregistrar.driver.did.btcr.util.validators;

import org.apache.commons.lang3.StringUtils;
import uniregistrar.driver.did.btcr.DriverConfigs;
import uniregistrar.driver.did.btcr.util.ParsingUtils;
import uniregistrar.request.DeactivateRequest;

public final class DeactivationRequestValidator {

    public static void validate(final DeactivateRequest request, final DriverConfigs configs) throws ValidationException {

        if (StringUtils.isEmpty(request.getIdentifier())) {
            throw new ValidationException("Deactivation request has no identifier");
        }

        if (request.getOptions() == null) {
            throw new ValidationException("Request options are null!");
        }
        String chain = ParsingUtils.parseChain(request.getOptions());
        ValidationCommons.validateChain(chain, configs);
    }

}