package uniregistrar.driver.did.btcr.util;

import uniregistrar.RegistrationException;

public class ResolvingException extends RegistrationException {

    public ResolvingException() {
        super();
    }

    public ResolvingException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

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
