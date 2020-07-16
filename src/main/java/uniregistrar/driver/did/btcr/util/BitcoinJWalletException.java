package uniregistrar.driver.did.btcr.util;

public class BitcoinJWalletException extends Exception {
	public BitcoinJWalletException(String msg, Throwable err) {
		super(msg, err);
	}

	public BitcoinJWalletException(String msg) {
		super(msg);
	}

	public BitcoinJWalletException(Throwable err) {
		super(err);
	}
}
