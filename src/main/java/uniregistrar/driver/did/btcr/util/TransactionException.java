package uniregistrar.driver.did.btcr.util;

public class TransactionException extends Exception {
	public TransactionException(String msg, Throwable err) {
		super(msg, err);
	}

	public TransactionException(String msg) {
		super(msg);
	}

	public TransactionException(Throwable err) {
		super(err);
	}
}