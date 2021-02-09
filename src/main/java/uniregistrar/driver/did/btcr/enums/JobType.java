package uniregistrar.driver.did.btcr.enums;

import com.google.common.base.Preconditions;

/**
 * The Enum class to differentiate DidBtcrJobs
 * 
 * @see uniregistrar.driver.did.btcr.DidBtcrJob
 */
public enum JobType {
	CREATE, UPDATE, DEACTIVATE;

	public static JobType fromString(String jobType) {
		Preconditions.checkNotNull(jobType, "Job type type is null");
		switch (jobType.toUpperCase()) {
		case "CREATE":
			return CREATE;
		case "UPDATE":
			return UPDATE;
		case "DEACTIVATE":
			return DEACTIVATE;
		default:
			throw new IllegalArgumentException("Invalid job type!");
		}
	}
}