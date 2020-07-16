package uniregistrar.driver.did.btcr.enums;

import com.google.common.base.Preconditions;

/**
 * The enum for funding types.
 */
public enum FundingType {
	USER, REQUEST, SERVER, DONATION;

	public static FundingType fromString(String fundingType) {
		Preconditions.checkNotNull(fundingType, "Funding type is null");
		switch (fundingType.toUpperCase()) {
		case "USER":
			return USER;
		case "REQUEST":
			return REQUEST;
		case "SERVER":
			return SERVER;
		case "DONATION":
			return DONATION;
		default:
			throw new IllegalArgumentException("Invalid funding type!");
		}
	}
}
