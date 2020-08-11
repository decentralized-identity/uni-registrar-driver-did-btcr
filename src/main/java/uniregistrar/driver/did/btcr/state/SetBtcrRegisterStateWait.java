package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.SetRegisterState;
import uniregistrar.state.UpdateState;

public final class SetBtcrRegisterStateWait {
	private SetBtcrRegisterStateWait() {
	}

	public static String getStateWait(RegisterState registerState) {

		if (!isStateWait(registerState))
			return null;
		return (String) registerState.getDidState().get("wait");
	}

	public static boolean isStateWait(RegisterState registerState) {

		return "wait".equals(SetRegisterState.getState(registerState));
	}

	public static String getStateWait(UpdateState updateState) {

		if (!isStateWait(updateState))
			return null;
		return (String) updateState.getDidState().get("wait");
	}

	public static boolean isStateWait(UpdateState updateState) {

		return "wait".equals(SetBtcrRegisterState.getState(updateState));
	}

	public static String getStateWait(DeactivateState deactivateState) {

		if (!isStateWait(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("wait");
	}

	public static boolean isStateWait(DeactivateState deactivateState) {

		return "wait".equals(SetBtcrRegisterState.getState(deactivateState));
	}

	public static String getStateWaittime(RegisterState registerState) {

		if (!isStateWait(registerState))
			return null;
		return (String) registerState.getDidState().get("waittime");
	}

	public static String getStateWaittime(UpdateState updateState) {

		if (!isStateWait(updateState))
			return null;
		return (String) updateState.getDidState().get("waittime");
	}

	public static String getStateWaittime(DeactivateState deactivateState) {

		if (!isStateWait(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("waittime");
	}

	public static void setStateWait(RegisterState registerState, String wait, String waittime) {

		SetBtcrRegisterState.setState(registerState, "wait");
		registerState.getDidState().put("wait", wait);
		registerState.getDidState().put("waittime", waittime);
	}

	public static void setStateWait(UpdateState updateState, String wait, String waittime) {

		SetBtcrRegisterState.setState(updateState, "wait");
		updateState.getDidState().put("wait", wait);
		updateState.getDidState().put("waittime", waittime);
	}

	public static void setStateWait(DeactivateState deactivateState, String wait, String waittime) {

		SetBtcrRegisterState.setState(deactivateState, "wait");
		deactivateState.getDidState().put("wait", wait);
		deactivateState.getDidState().put("waittime", waittime);
	}

}
