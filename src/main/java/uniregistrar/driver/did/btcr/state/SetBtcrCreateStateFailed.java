package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.CreateState;
import uniregistrar.state.DeactivateState;
import uniregistrar.state.SetState;
import uniregistrar.state.UpdateState;

public class SetBtcrCreateStateFailed {

	public static String getStateFail(CreateState createState) {

		if (!isStateFail(createState))
			return null;
		return (String) createState.getDidState().get("failed");
	}

	public static boolean isStateFail(CreateState createState) {

		return "failed".equals(SetState.getState(createState));
	}

	public static String getStateFail(UpdateState updateState) {

		if (!isStateFail(updateState))
			return null;
		return (String) updateState.getDidState().get("failed");
	}

	public static boolean isStateFail(UpdateState updateState) {

		return "failed".equals(SetBtcrCreateState.getState(updateState));
	}

	public static String getStateFail(DeactivateState deactivateState) {

		if (!isStateFail(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("failed");
	}

	public static boolean isStateFail(DeactivateState deactivateState) {

		return "failed".equals(SetBtcrCreateState.getState(deactivateState));
	}

	public static String getStateInitTime(CreateState createState) {

		if (!isStateFail(createState))
			return null;
		return (String) createState.getDidState().get("initTime");
	}

	public static String getStateInitTime(UpdateState updateState) {

		if (!isStateFail(updateState))
			return null;
		return (String) updateState.getDidState().get("initTime");
	}

	public static String getStateInitTime(DeactivateState deactivateState) {

		if (!isStateFail(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("initTime");
	}

	public static void setStateFail(CreateState createState, String reason, String initTime) {

		SetBtcrCreateState.setState(createState, "failed");
		createState.getDidState().remove("wait");
		createState.getDidState().remove("waittime");
		createState.getDidState().put("reason", reason);
		createState.getDidState().put("initTime", initTime);
	}

	public static void setStateFail(UpdateState updateState, String reason, String initTime) {

		SetBtcrCreateState.setState(updateState, "failed");
		updateState.getDidState().remove("wait");
		updateState.getDidState().remove("waittime");
		updateState.getDidState().put("reason", reason);
		updateState.getDidState().put("initTime", initTime);
	}

	public static void setStateFail(DeactivateState deactivateState, String reason, String initTime) {

		SetBtcrCreateState.setState(deactivateState, "failed");
		deactivateState.getDidState().remove("wait");
		deactivateState.getDidState().remove("waittime");
		deactivateState.getDidState().put("reason", reason);
		deactivateState.getDidState().put("initTime", initTime);
	}
}
