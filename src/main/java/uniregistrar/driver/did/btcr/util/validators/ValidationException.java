package uniregistrar.driver.did.btcr.util.validators;

public class ValidationException extends Exception {
	public ValidationException(String msg, Throwable err) {
		super(msg, err);
	}

	public ValidationException(String msg) {
		super(msg);
	}

	public ValidationException(Throwable err) {
		super(err);
	}
}
