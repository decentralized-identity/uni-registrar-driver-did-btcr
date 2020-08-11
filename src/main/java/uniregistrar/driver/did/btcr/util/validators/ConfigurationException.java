package uniregistrar.driver.did.btcr.util.validators;

public class ConfigurationException extends Exception {

	public ConfigurationException(String msg, Throwable err) {
		super(msg, err);
	}

	public ConfigurationException(String msg) {
		super(msg);
	}

	public ConfigurationException(Throwable err) {
		super(err);
	}
}
