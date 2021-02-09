package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

public final class SetBtcrCreateState {
	private SetBtcrCreateState() {

	}

	public static String getState(CreateState createState) {

		return (String) createState.getDidState().get("state");
	}

	public static String getState(UpdateState updateState) {

		return (String) updateState.getDidState().get("state");
	}

	public static String getState(DeactivateState deactivateState) {

		return (String) deactivateState.getDidState().get("state");
	}

	public static void setState(CreateState createState, String state) {

		createState.getDidState().put("state", state);
	}

	public static void setState(UpdateState updateState, String state) {

		updateState.getDidState().put("state", state);
	}

	public static void setState(DeactivateState deactivateState, String state) {

		deactivateState.getDidState().put("state", state);
	}
}
