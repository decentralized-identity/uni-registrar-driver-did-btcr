package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.SetRegisterState;
import uniregistrar.state.UpdateState;

public class SetBtcrRegisterStateFailed {

    public static String getStateFail(RegisterState registerState) {

        if (!isStateFail(registerState)) return null;
        return (String) registerState.getDidState().get("failed");
    }

    public static boolean isStateFail(RegisterState registerState) {

        return "failed".equals(SetRegisterState.getState(registerState));
    }

    public static String getStateFail(UpdateState updateState) {

        if (!isStateFail(updateState)) return null;
        return (String) updateState.getDidState().get("failed");
    }

    public static boolean isStateFail(UpdateState updateState) {

        return "failed".equals(SetBtcrRegisterState.getState(updateState));
    }

    public static String getStateFail(DeactivateState deactivateState) {

        if (!isStateFail(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("failed");
    }

    public static boolean isStateFail(DeactivateState deactivateState) {

        return "failed".equals(SetBtcrRegisterState.getState(deactivateState));
    }

    public static String getStateInitTime(RegisterState registerState) {

        if (!isStateFail(registerState)) return null;
        return (String) registerState.getDidState().get("initTime");
    }

    public static String getStateInitTime(UpdateState updateState) {

        if (!isStateFail(updateState)) return null;
        return (String) updateState.getDidState().get("initTime");
    }

    public static String getStateInitTime(DeactivateState deactivateState) {

        if (!isStateFail(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("initTime");
    }

    public static void setStateFail(RegisterState registerState, String reason, String initTime) {

        SetBtcrRegisterState.setState(registerState, "failed");
        registerState.getDidState().remove("wait");
        registerState.getDidState().remove("waittime");
        registerState.getDidState().put("reason", reason);
        registerState.getDidState().put("initTime", initTime);
    }

    public static void setStateFail(UpdateState updateState, String reason, String initTime) {

        SetBtcrRegisterState.setState(updateState, "failed");
        updateState.getDidState().remove("wait");
        updateState.getDidState().remove("waittime");
        updateState.getDidState().put("reason", reason);
        updateState.getDidState().put("initTime", initTime);
    }

    public static void setStateFail(DeactivateState deactivateState, String reason, String initTime) {

        SetBtcrRegisterState.setState(deactivateState, "failed");
        deactivateState.getDidState().remove("wait");
        deactivateState.getDidState().remove("waittime");
        deactivateState.getDidState().put("reason", reason);
        deactivateState.getDidState().put("initTime", initTime);
    }
}
