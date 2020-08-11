package uniregistrar.driver.did.btcr.funding;

public class FundingException extends Exception {

	public FundingException(String msg, Throwable err) {
		super(msg, err);
	}

	public FundingException(String msg) {
		super(msg);
	}

	public FundingException(Throwable err) {
		super(err);
	}
}
