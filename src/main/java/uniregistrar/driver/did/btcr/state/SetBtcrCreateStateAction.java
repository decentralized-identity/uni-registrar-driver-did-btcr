package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

public final class SetBtcrCreateStateAction {
	private SetBtcrCreateStateAction() {

	}

	public static String getStateAction(CreateState createState) {

		if (!isStateAction(createState))
			return null;
		return (String) createState.getDidState().get("action");
	}

	public static boolean isStateAction(CreateState createState) {

		return "action".equals(SetBtcrCreateState.getState(createState));
	}

	public static String getStateAction(UpdateState updateState) {

		if (!isStateAction(updateState))
			return null;
		return (String) updateState.getDidState().get("action");
	}

	public static boolean isStateAction(UpdateState updateState) {

		return "action".equals(SetBtcrCreateState.getState(updateState));
	}

	public static String getStateAction(DeactivateState deactivateState) {

		if (!isStateAction(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("action");
	}

	public static boolean isStateAction(DeactivateState deactivateState) {

		return "action".equals(SetBtcrCreateState.getState(deactivateState));
	}

	public static void setStateAction(CreateState createState, String action) {

		SetBtcrCreateState.setState(createState, "action");
		createState.getDidState().put("action", action);
	}

	public static void setStateAction(UpdateState updateState, String action) {

		SetBtcrCreateState.setState(updateState, "action");
		updateState.getDidState().put("action", action);
	}

	public static void setStateAction(DeactivateState deactivateState, String action) {

		SetBtcrCreateState.setState(deactivateState, "action");
		deactivateState.getDidState().put("action", action);
	}
}
