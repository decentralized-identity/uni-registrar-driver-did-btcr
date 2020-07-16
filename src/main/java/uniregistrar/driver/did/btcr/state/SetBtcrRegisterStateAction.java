package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

public final class SetBtcrRegisterStateAction {
    private SetBtcrRegisterStateAction() {

    }

    public static String getStateAction(RegisterState registerState) {

        if (!isStateAction(registerState)) return null;
        return (String) registerState.getDidState().get("action");
    }

    public static boolean isStateAction(RegisterState registerState) {

        return "action".equals(SetBtcrRegisterState.getState(registerState));
    }

    public static String getStateAction(UpdateState updateState) {

        if (!isStateAction(updateState)) return null;
        return (String) updateState.getDidState().get("action");
    }

    public static boolean isStateAction(UpdateState updateState) {

        return "action".equals(SetBtcrRegisterState.getState(updateState));
    }

    public static String getStateAction(DeactivateState deactivateState) {

        if (!isStateAction(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("action");
    }

    public static boolean isStateAction(DeactivateState deactivateState) {

        return "action".equals(SetBtcrRegisterState.getState(deactivateState));
    }

    public static void setStateAction(RegisterState registerState, String action) {

        SetBtcrRegisterState.setState(registerState, "action");
        registerState.getDidState().put("action", action);
    }

    public static void setStateAction(UpdateState updateState, String action) {

        SetBtcrRegisterState.setState(updateState, "action");
        updateState.getDidState().put("action", action);
    }

    public static void setStateAction(DeactivateState deactivateState, String action) {

        SetBtcrRegisterState.setState(deactivateState, "action");
        deactivateState.getDidState().put("action", action);
    }
}
