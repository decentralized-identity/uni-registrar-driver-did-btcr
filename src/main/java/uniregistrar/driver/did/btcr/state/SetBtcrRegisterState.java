package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

public final class SetBtcrRegisterState {
	private SetBtcrRegisterState() {

	}

	public static String getState(RegisterState registerState) {

		return (String) registerState.getDidState().get("state");
	}

	public static String getState(UpdateState updateState) {

		return (String) updateState.getDidState().get("state");
	}

	public static String getState(DeactivateState deactivateState) {

		return (String) deactivateState.getDidState().get("state");
	}

	public static void setState(RegisterState registerState, String state) {

		registerState.getDidState().put("state", state);
	}

	public static void setState(UpdateState updateState, String state) {

		updateState.getDidState().put("state", state);
	}

	public static void setState(DeactivateState deactivateState, String state) {

		deactivateState.getDidState().put("state", state);
	}
}
