package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

public final class SetCreateStateWaitConfirm {

	private SetCreateStateWaitConfirm() {

	}

	public static boolean isStateWaitConfirm(CreateState createState) {

		return "confirm".equals(SetBtcrCreateStateWait.getStateWait(createState));
	}

	public static boolean isStateWaitConfirm(UpdateState updateState) {

		return "confirm".equals(SetBtcrCreateStateWait.getStateWait(updateState));
	}

	public static boolean isStateWaitConfirm(DeactivateState deactivateState) {

		return "confirm".equals(SetBtcrCreateStateWait.getStateWait(deactivateState));
	}

	public static void setStateWaitConfirm(CreateState createState) {

		SetBtcrCreateStateWait.setStateWait(createState, "confirm", "3600000");
	}

	public static void setStateWaitConfirm(UpdateState updateState) {

		SetBtcrCreateStateWait.setStateWait(updateState, "confirm", "3600000");
	}

	public static void setStateWaitConfirm(DeactivateState deactivateState) {

		SetBtcrCreateStateWait.setStateWait(deactivateState, "confirm", "3600000");
	}

	public static void setStateWaitConfirm(CreateState createState, String msg) {

		SetBtcrCreateStateWait.setStateWait(createState, "confirm", msg);
	}

	public static void setStateWaitConfirm(UpdateState updateState, String msg) {

		SetBtcrCreateStateWait.setStateWait(updateState, "confirm", msg);
	}

	public static void setStateWaitConfirm(DeactivateState deactivateState, String msg) {

		SetBtcrCreateStateWait.setStateWait(deactivateState, "confirm", msg);
	}
}
