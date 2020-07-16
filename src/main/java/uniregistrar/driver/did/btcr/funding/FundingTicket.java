package uniregistrar.driver.did.btcr.funding;

import java.util.UUID;

import com.google.common.base.MoreObjects;

public class FundingTicket {

	private final UUID ticketID;
	private BtcrFund fund;
	private boolean expired;

	public FundingTicket(BtcrFund fund) {
		this.fund = fund;
		ticketID = UUID.randomUUID();
	}

	public BtcrFund getFund() {
		return fund;
	}

	public void setFund(BtcrFund fund) {
		this.fund = fund;
	}

	public UUID getTicketID() {
		return ticketID;
	}

	public boolean isExpired() {
		return expired;
	}

	public String toString() {
		return MoreObjects.toStringHelper(this).add("fund", fund).add("ticketID", ticketID).toString();
	}
}
