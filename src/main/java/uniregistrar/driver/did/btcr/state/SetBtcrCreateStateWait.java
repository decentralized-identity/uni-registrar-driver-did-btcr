package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.SetCreateState;
import uniregistrar.state.UpdateState;

public final class SetBtcrCreateStateWait {
	private SetBtcrCreateStateWait() {
	}

	public static String getStateWait(CreateState createState) {

		if (!isStateWait(createState))
			return null;
		return (String) createState.getDidState().get("wait");
	}

	public static boolean isStateWait(CreateState createState) {

		return "wait".equals(SetCreateState.getState(createState));
	}

	public static String getStateWait(UpdateState updateState) {

		if (!isStateWait(updateState))
			return null;
		return (String) updateState.getDidState().get("wait");
	}

	public static boolean isStateWait(UpdateState updateState) {

		return "wait".equals(SetBtcrCreateState.getState(updateState));
	}

	public static String getStateWait(DeactivateState deactivateState) {

		if (!isStateWait(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("wait");
	}

	public static boolean isStateWait(DeactivateState deactivateState) {

		return "wait".equals(SetBtcrCreateState.getState(deactivateState));
	}

	public static String getStateWaittime(CreateState createState) {

		if (!isStateWait(createState))
			return null;
		return (String) createState.getDidState().get("waittime");
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

	public static void setStateWait(CreateState createState, String wait, String waittime) {

		SetBtcrCreateState.setState(createState, "wait");
		createState.getDidState().put("wait", wait);
		createState.getDidState().put("waittime", waittime);
	}

	public static void setStateWait(UpdateState updateState, String wait, String waittime) {

		SetBtcrCreateState.setState(updateState, "wait");
		updateState.getDidState().put("wait", wait);
		updateState.getDidState().put("waittime", waittime);
	}

	public static void setStateWait(DeactivateState deactivateState, String wait, String waittime) {

		SetBtcrCreateState.setState(deactivateState, "wait");
		deactivateState.getDidState().put("wait", wait);
		deactivateState.getDidState().put("waittime", waittime);
	}

}
