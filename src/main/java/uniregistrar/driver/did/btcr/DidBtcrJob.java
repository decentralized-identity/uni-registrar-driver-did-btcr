package uniregistrar.driver.did.btcr;

import com.google.common.base.MoreObjects;
import foundation.identity.did.Service;
import foundation.identity.did.VerificationMethod;
import info.weboftrust.btctxlookup.Chain;
import org.bitcoinj.core.ECKey;
import uniregistrar.driver.did.btcr.enums.FundingType;
import uniregistrar.driver.did.btcr.enums.JobType;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DidBtcrJob {
	private String jobId;
	private final Chain chain;
	private final String transactionHash;
	private final URI didContinuationUri;
	private final ECKey privateKey;
	private final ECKey changeKey;
	private final List<Service> addServices;
	private final List<VerificationMethod> addVerificationMethods;
	private final List<VerificationMethod> addAuthentications;
	private final JobType jobType;
	private final boolean rotateKey;
	private final long creationTime;
	private final FundingType fundingType;
	private String identifier = null;

	public DidBtcrJob(Chain chain, String transactionHash, URI didContinuationUri, ECKey privateKey, ECKey changeKey,
					  List<Service> addServices, List<VerificationMethod> addVerificationMethods,
					  List<VerificationMethod> addAuthentications, JobType jobType, boolean rotateKey, FundingType fundingType) {

		this(UUID.randomUUID().toString(), chain, transactionHash, didContinuationUri, privateKey, changeKey, addServices, addVerificationMethods,
			 addAuthentications, jobType, rotateKey, fundingType);
	}

	public DidBtcrJob(String jobId, Chain chain, String transactionHash, URI didContinuationUri, ECKey privateKey, ECKey changeKey,
					  List<Service> addServices, List<VerificationMethod> addVerificationMethods,
					  List<VerificationMethod> addAuthentications, JobType jobType, boolean rotateKey,
					  FundingType fundingType) {
		this.jobId = jobId;
		this.chain = chain;
		this.transactionHash = transactionHash;
		this.didContinuationUri = didContinuationUri;
		this.privateKey = privateKey;
		this.changeKey = changeKey;
		this.addServices = addServices == null ? null : Collections.unmodifiableList(addServices);
		this.addVerificationMethods = addVerificationMethods == null ? null
																	 : Collections.unmodifiableList(addVerificationMethods);
		this.addAuthentications = addAuthentications == null ? null : Collections.unmodifiableList(addAuthentications);
		this.jobType = jobType;
		this.rotateKey = rotateKey;
		this.creationTime = Instant.now().getEpochSecond();
		this.fundingType = fundingType;
	}

	public FundingType getFundingType() {
		return fundingType;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
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

	public List<VerificationMethod> getAddAuthentications() {
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
