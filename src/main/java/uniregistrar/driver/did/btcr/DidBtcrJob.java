package uniregistrar.driver.did.btcr;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import foundation.identity.did.Authentication;
import foundation.identity.did.Service;
import foundation.identity.did.VerificationMethod;
import org.bitcoinj.core.ECKey;

import com.google.common.base.MoreObjects;

import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.enums.FundingType;
import uniregistrar.driver.did.btcr.enums.JobType;

public class DidBtcrJob {
	private final String jobId;
	private final Chain chain;
	private final String transactionHash;
	private final URI didContinuationUri;
	private final ECKey privateKey;
	private final ECKey changeKey;
	private final List<Service> addServices;
	private final List<VerificationMethod> addVerificationMethods;
	private final List<Authentication> addAuthentications;
	private final JobType jobType;
	private final boolean rotateKey;
	private final long creationTime;
	private final FundingType fundingType;
	private String identifier = null;

	public DidBtcrJob(Chain chain, String transactionHash, URI didContinuationUri, ECKey privateKey, ECKey changeKey,
			List<Service> addServices, List<VerificationMethod> addVerificationMethods,
			List<Authentication> addAuthentications, JobType jobType, boolean rotateKey, FundingType fundingType) {
		this.jobId = UUID.randomUUID().toString();
		this.chain = chain;
		this.transactionHash = transactionHash;
		this.didContinuationUri = didContinuationUri;
		this.privateKey = privateKey;
		this.addServices = addServices == null ? null : Collections.unmodifiableList(addServices);
		this.addVerificationMethods = addVerificationMethods == null ? null
				: Collections.unmodifiableList(addVerificationMethods);
		this.addAuthentications = addAuthentications == null ? null : Collections.unmodifiableList(addAuthentications);
		this.jobType = jobType;
		this.changeKey = changeKey;
		this.rotateKey = rotateKey;
		this.fundingType = fundingType;
		this.creationTime = Instant.now().getEpochSecond();
	}

	public FundingType getFundingType() {
		return fundingType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public boolean isRotateKey() {
		return rotateKey;
	}

	public JobType getJobType() {
		return jobType;
	}

	public String getJobId() {
		return jobId;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public String getTransactionHash() {

		return transactionHash;
	}

	public Chain getChain() {
		return chain;
	}

	public URI getDidContinuationUri() {

		return didContinuationUri;
	}

	public ECKey getPrivateKey() {

		return privateKey;
	}

	public List<Service> getAddServices() {
		return addServices;
	}

	public List<VerificationMethod> getAddVerificationMethods() {
		return addVerificationMethods;
	}

	public List<Authentication> getAddAuthentications() {
		return addAuthentications;
	}

	public ECKey getChangeKey() {
		return changeKey;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("jobId", jobId).add("identifier", identifier)
				.add("chain", chain.toString()).add("transactionHash", transactionHash)
				.add("didContinuationUri", didContinuationUri).add("privateKey", privateKey).add("changeKey", changeKey)
				.add("addServices", addServices).add("addVerificationMethods", addVerificationMethods)
				.add("addAuthentications", addAuthentications).add("jobType", jobType).add("rotateKey", rotateKey)
				.add("creationTime", creationTime).toString();
	}
}
