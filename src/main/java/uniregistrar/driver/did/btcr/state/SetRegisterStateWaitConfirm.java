package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

public final class SetRegisterStateWaitConfirm {

    private SetRegisterStateWaitConfirm() {

    }

    public static boolean isStateWaitConfirm(RegisterState registerState) {

        return "confirm".equals(SetBtcrRegisterStateWait.getStateWait(registerState));
    }

    public static boolean isStateWaitConfirm(UpdateState updateState) {

        return "confirm".equals(SetBtcrRegisterStateWait.getStateWait(updateState));
    }

    public static boolean isStateWaitConfirm(DeactivateState deactivateState) {

        return "confirm".equals(SetBtcrRegisterStateWait.getStateWait(deactivateState));
    }

    public static void setStateWaitConfirm(RegisterState registerState) {

        SetBtcrRegisterStateWait.setStateWait(registerState, "confirm", "3600000");
    }

    public static void setStateWaitConfirm(UpdateState updateState) {

        SetBtcrRegisterStateWait.setStateWait(updateState, "confirm", "3600000");
    }

    public static void setStateWaitConfirm(DeactivateState deactivateState) {

        SetBtcrRegisterStateWait.setStateWait(deactivateState, "confirm", "3600000");
    }

    public static void setStateWaitConfirm(RegisterState registerState, String msg) {

        SetBtcrRegisterStateWait.setStateWait(registerState, "confirm", msg);
    }

    public static void setStateWaitConfirm(UpdateState updateState, String msg) {

        SetBtcrRegisterStateWait.setStateWait(updateState, "confirm", msg);
    }

    public static void setStateWaitConfirm(DeactivateState deactivateState, String msg) {

        SetBtcrRegisterStateWait.setStateWait(deactivateState, "confirm", msg);
    }
}
