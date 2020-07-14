package uniregistrar.driver.did.btcr.funding;

import java.time.Instant;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;

import com.google.common.base.MoreObjects;

import uniregistrar.driver.did.btcr.enums.FundingType;

public class BtcrFund {

	private final long creationTime;
	private String txID;
	private String uuid;
	private FundingType fundingType;
	private ECKey fundingKey;
	private ECKey changeKey;
	private Address changeAddress;
	private Address fundingAddress;
	private Coin amount;
	private int outIndex;
	private TransactionOutput transactionOutput;
	private Script.ScriptType scriptType;

	public BtcrFund() {
		creationTime = Instant.now().getEpochSecond();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("creationTime", creationTime).add("txID", txID).add("uuid", uuid)
				.add("fundingType", fundingType).add("fundingKey", fundingKey).add("changeKey", changeKey)
				.add("changeAddress", changeAddress).add("fundingAddress", fundingAddress).add("amount", amount)
				.add("outIndex", outIndex).add("transactionOutput", transactionOutput).add("scriptType", scriptType)
				.toString();
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Address getFundingAddress() {
		return fundingAddress;
	}

	public void setFundingAddress(Address fundingAddress) {
		this.fundingAddress = fundingAddress;
	}

	public Coin getAmount() {
		return amount;
	}

	public void setAmount(Coin amount) {
		this.amount = amount;
	}

	public int getOutIndex() {
		return outIndex;
	}

	public void setOutIndex(int outIndex) {
		this.outIndex = outIndex;
	}

	public TransactionOutput getTransactionOutput() {
		return transactionOutput;
	}

	public void setTransactionOutput(TransactionOutput transactionOutput) {
		this.transactionOutput = transactionOutput;
	}

	public FundingType getFundingType() {
		return fundingType;
	}

	public void setFundingType(FundingType fundingType) {
		this.fundingType = fundingType;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public String getTxID() {
		return txID;
	}

	public void setTxID(String txID) {
		this.txID = txID;
	}

	public ECKey getFundingKey() {
		return fundingKey;
	}

	public void setFundingKey(ECKey fundingKey) {
		this.fundingKey = fundingKey;
	}

	public ECKey getChangeKey() {
		return changeKey;
	}

	public void setChangeKey(ECKey changeKey) {
		this.changeKey = changeKey;
	}

	public Address getChangeAddress() {
		return changeAddress;
	}

	public void setChangeAddress(Address changeAddress) {
		this.changeAddress = changeAddress;
	}

	public Address getFundAddress() {
		return fundingAddress;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public Script.ScriptType getScriptType() {
		return scriptType;
	}

	public void setScriptType(Script.ScriptType scriptType) {
		this.scriptType = scriptType;
	}

	public static final class Builder {
		private long creationTime;
		private String txID;
		private String uuid;
		private FundingType fundingType;
		private ECKey fundingKey;
		private long satValue;
		private TransactionOutPoint txOutPoint;
		private ECKey changeKey;
		private Address changeAddress;
		private Address fundingAddress;
		private Coin amount;
		private int outIndex;
		private TransactionOutput transactionOutput;
		private Script.ScriptType scriptType;

		private Builder() {
		}

		public static Builder newFund() {
			return new Builder();
		}

		public Builder withTxID(String txID) {
			this.txID = txID;
			return this;
		}

		public Builder withUuid(String uuid) {
			this.uuid = uuid;
			return this;
		}

		public Builder withFundingType(FundingType fundingType) {
			this.fundingType = fundingType;
			return this;
		}

		public Builder withFundingKey(ECKey fundingKey) {
			this.fundingKey = fundingKey;
			return this;
		}

		public Builder withSatValue(long satValue) {
			this.satValue = satValue;
			return this;
		}

		public Builder withTxOutPoint(TransactionOutPoint txOutPoint) {
			this.txOutPoint = txOutPoint;
			return this;
		}

		public Builder withChangeKey(ECKey changeKey) {
			this.changeKey = changeKey;
			return this;
		}

		public Builder withChangeAddress(Address changeAddress) {
			this.changeAddress = changeAddress;
			return this;
		}

		public Builder withFundingAddress(Address fundingAddress) {
			this.fundingAddress = fundingAddress;
			return this;
		}

		public Builder withAmount(Coin amount) {
			this.amount = amount;
			return this;
		}

		public Builder withOutIndex(int outIndex) {
			this.outIndex = outIndex;
			return this;
		}

		public Builder withTransactionOutput(TransactionOutput transactionOutput) {
			this.transactionOutput = transactionOutput;
			return this;
		}

		public Builder withScriptType(Script.ScriptType scriptType) {
			this.scriptType = scriptType;
			return this;
		}

		public BtcrFund build() {
			BtcrFund btcrFund = new BtcrFund();
			btcrFund.setTxID(txID);
			btcrFund.setUuid(uuid);
			btcrFund.setFundingType(fundingType);
			btcrFund.setFundingKey(fundingKey);
			btcrFund.setChangeKey(changeKey);
			btcrFund.setChangeAddress(changeAddress);
			btcrFund.setFundingAddress(fundingAddress);
			btcrFund.setAmount(amount);
			btcrFund.setOutIndex(outIndex);
			btcrFund.setTransactionOutput(transactionOutput);
			btcrFund.setScriptType(scriptType);
			return btcrFund;
		}
	}
}
