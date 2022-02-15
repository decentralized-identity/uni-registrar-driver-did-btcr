package uniregistrar.driver.did.btcr.state;

import java.util.Map;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

/**
 * Just another messy class since we don't have state interface
 */
public final class SetBtcrCreateStateFinished {
	private SetBtcrCreateStateFinished() {

	}

	public static String getStateFinishedDid(CreateState createState) {
		if (!isStateFinished(createState))
			return null;
		return (String) createState.getDidState().get("did");
	}

	public static boolean isStateFinished(CreateState createState) {

		return "finished".equals(SetBtcrCreateState.getState(createState));
	}

	public static String getStateFinishedDid(UpdateState updateState) {
		if (!isStateFinished(updateState))
			return null;
		return (String) updateState.getDidState().get("did");
	}

	public static boolean isStateFinished(UpdateState updateState) {

		return "finished".equals(SetBtcrCreateState.getState(updateState));
	}

	public static String getStateFinishedDid(DeactivateState deactivateState) {
		if (!isStateFinished(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("did");
	}

	public static boolean isStateFinished(DeactivateState deactivateState) {

		return "finished".equals(SetBtcrCreateState.getState(deactivateState));
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getStateFinishedSecret(CreateState createState) {

		if (!isStateFinished(createState))
			return null;
		return (Map<String, Object>) createState.getDidState().get("secret");
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getStateFinishedSecret(UpdateState updateState) {

		if (!isStateFinished(updateState))
			return null;
		return (Map<String, Object>) updateState.getDidState().get("secret");
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getStateFinishedSecret(DeactivateState deactivateState) {

		if (!isStateFinished(deactivateState))
			return null;
		return (Map<String, Object>) deactivateState.getDidState().get("secret");
	}

	public static void setStateFinished(CreateState createState, String did, Map<String, Object> secret) {

		SetBtcrCreateState.setState(createState, "finished");
		createState.getDidState().put("did", did);
		createState.getDidState().put("secret", secret);
	}

	public static void setStateFinished(UpdateState updateState, String did, Map<String, Object> secret) {

		SetBtcrCreateState.setState(updateState, "finished");
		updateState.getDidState().put("did", did);
		updateState.getDidState().put("secret", secret);
	}

	public static void setStateFinished(DeactivateState deactivateState, String did,
			Map<String, Object> secret) {

		SetBtcrCreateState.setState(deactivateState, "finished");
		deactivateState.getDidState().put("did", did);
		deactivateState.getDidState().put("secret", secret);
	}
}
