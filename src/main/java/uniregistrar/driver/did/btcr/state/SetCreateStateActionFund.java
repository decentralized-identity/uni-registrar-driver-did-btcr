package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.CreateState;
import uniregistrar.state.UpdateState;

public final class SetCreateStateActionFund {

	private SetCreateStateActionFund() {

	}

	public static String getStateActionFundBitcoinAddress(CreateState createState) {

		if (!isStateActionFund(createState))
			return null;
		return (String) createState.getDidState().get("bitcoinAddress");
	}

	public static boolean isStateActionFund(CreateState createState) {

		return "fund".equals(SetBtcrCreateStateAction.getStateAction(createState));
	}

	public static String getStateActionFundBitcoinAddress(UpdateState updateState) {

		if (!isStateActionFund(updateState))
			return null;
		return (String) updateState.getDidState().get("bitcoinAddress");
	}

	public static boolean isStateActionFund(UpdateState updateState) {

		return "fund".equals(SetBtcrCreateStateAction.getStateAction(updateState));
	}

	public static String getStateActionFundBitcoinAddress(DeactivateState deactivateState) {

		if (!isStateActionFund(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("bitcoinAddress");
	}

	public static boolean isStateActionFund(DeactivateState deactivateState) {

		return "fund".equals(SetBtcrCreateStateAction.getStateAction(deactivateState));
	}

	public static String getStateActionFundSatoshis(CreateState createState) {

		if (!isStateActionFund(createState))
			return null;
		return (String) createState.getDidState().get("satoshis");
	}

	public static String getStateActionFundSatoshis(UpdateState updateState) {

		if (!isStateActionFund(updateState))
			return null;
		return (String) updateState.getDidState().get("satoshis");
	}

	public static String getStateActionFundSatoshis(DeactivateState deactivateState) {

		if (!isStateActionFund(deactivateState))
			return null;
		return (String) deactivateState.getDidState().get("satoshis");
	}

	public static void setStateActionFund(CreateState createState, String bitcoinAddress, String satoshis) {

		SetBtcrCreateStateAction.setStateAction(createState, "fund");
		createState.getDidState().put("bitcoinAddress", bitcoinAddress);
		createState.getDidState().put("satoshis", satoshis);
	}

	public static void setStateActionFund(UpdateState updateState, String bitcoinAddress, String satoshis) {

		SetBtcrCreateStateAction.setStateAction(updateState, "fund");
		updateState.getDidState().put("bitcoinAddress", bitcoinAddress);
		updateState.getDidState().put("satoshis", satoshis);
	}

	public static void setStateActionFund(DeactivateState deactivateState, String bitcoinAddress, String satoshis) {

		SetBtcrCreateStateAction.setStateAction(deactivateState, "fund");
		deactivateState.getDidState().put("bitcoinAddress", bitcoinAddress);
		deactivateState.getDidState().put("satoshis", satoshis);
	}
}
