package uniregistrar.driver.did.btcr.util;

public class WalletException extends Exception {

	public WalletException(String msg, Throwable err) {
		super(msg, err);
	}

	public WalletException(String msg) {
		super(msg);
	}

	public WalletException(Throwable err) {
		super(err);
	}
}
