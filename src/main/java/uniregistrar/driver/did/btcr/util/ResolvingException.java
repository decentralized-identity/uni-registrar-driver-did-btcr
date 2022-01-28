package uniregistrar.driver.did.btcr.util;

import uniregistrar.RegistrationException;

public class ResolvingException extends RegistrationException {

	public ResolvingException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public ResolvingException(String arg0) {
		super(arg0);
	}

	public ResolvingException(Throwable arg0) {
		super(arg0);
	}
}
