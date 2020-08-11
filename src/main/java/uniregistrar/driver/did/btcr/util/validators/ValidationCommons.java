package uniregistrar.driver.did.btcr.util.validators;

import org.apache.commons.lang3.StringUtils;

import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.DriverConfigs;

/**
 * To be included when requests have a common interface or extended from a super
 * class
 */
public final class ValidationCommons {

	private ValidationCommons() {
	}

	public static void validateChain(String chain, DriverConfigs configs) throws ValidationException {
		validateChain(Chain.fromString(chain), configs);
	}

	public static void validateChain(Chain chain, DriverConfigs configs) throws ValidationException {
		switch (chain) {
		case MAINNET:
			if (!configs.isRunMainnet()) {
				throw new ValidationException("Mainnet is not running");
			}
			break;
		case TESTNET:
			if (!configs.isRunTestnet()) {
				throw new ValidationException("Testnet is not running");
			}
			break;
		case REGTESTNET:
			if (!configs.isRunRegtest()) {
				throw new ValidationException("Regtestnet is not running");
			}
			break;
		}
	}

	public static void validateWiF(String prvWiF) throws ValidationException {
		if (StringUtils.isEmpty(prvWiF)) {
			throw new ValidationException("Private key is null or empty!");
		}

		if (prvWiF.length() != 51 && prvWiF.length() != 52) {
			throw new ValidationException("Private key is invalid!");
		}
	}

	public static void validateTxString(final String txHash) throws ValidationException {
		if (StringUtils.isEmpty(txHash)) {
			throw new ValidationException(ValidationErrors.TX_HASH_EMPTY_OR_NULL);
		}
		if (txHash.length() != 64) {
			throw new ValidationException(ValidationErrors.TX_HASH_INVALID);
		}
		try {
			Long.parseLong(txHash, 16);
		} catch (NumberFormatException e) {
			throw new ValidationException(ValidationErrors.TX_HASH_INVALID);
		}
	}
}