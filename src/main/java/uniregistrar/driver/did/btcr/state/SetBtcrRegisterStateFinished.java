package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

import java.util.Map;

/**
 * Just another messy class since we don't have state interface
 */
public final class SetBtcrRegisterStateFinished {
    private SetBtcrRegisterStateFinished() {

    }

    public static String getStateFinishedIdentifier(RegisterState registerState) {
        if (!isStateFinished(registerState)) return null;
        return (String) registerState.getDidState().get("identifier");
    }

    public static boolean isStateFinished(RegisterState registerState) {

        return "finished".equals(SetBtcrRegisterState.getState(registerState));
    }

    public static String getStateFinishedIdentifier(UpdateState updateState) {
        if (!isStateFinished(updateState)) return null;
        return (String) updateState.getDidState().get("identifier");
    }

    public static boolean isStateFinished(UpdateState updateState) {

        return "finished".equals(SetBtcrRegisterState.getState(updateState));
    }

    public static String getStateFinishedIdentifier(DeactivateState deactivateState) {
        if (!isStateFinished(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("identifier");
    }

    public static boolean isStateFinished(DeactivateState deactivateState) {

        return "finished".equals(SetBtcrRegisterState.getState(deactivateState));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getStateFinishedSecret(RegisterState registerState) {

        if (!isStateFinished(registerState)) return null;
        return (Map<String, Object>) registerState.getDidState().get("secret");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getStateFinishedSecret(UpdateState updateState) {

        if (!isStateFinished(updateState)) return null;
        return (Map<String, Object>) updateState.getDidState().get("secret");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getStateFinishedSecret(DeactivateState deactivateState) {

        if (!isStateFinished(deactivateState)) return null;
        return (Map<String, Object>) deactivateState.getDidState().get("secret");
    }

    public static void setStateFinished(RegisterState registerState, String identifier, Map<String, Object> secret) {

        SetBtcrRegisterState.setState(registerState, "finished");
        registerState.getDidState().put("identifier", identifier);
        registerState.getDidState().put("secret", secret);
    }

    public static void setStateFinished(UpdateState updateState, String identifier, Map<String, Object> secret) {

        SetBtcrRegisterState.setState(updateState, "finished");
        updateState.getDidState().put("identifier", identifier);
        updateState.getDidState().put("secret", secret);
    }

    public static void setStateFinished(DeactivateState deactivateState, String identifier, Map<String, Object> secret) {

        SetBtcrRegisterState.setState(deactivateState, "finished");
        deactivateState.getDidState().put("identifier", identifier);
        deactivateState.getDidState().put("secret", secret);
    }
}
