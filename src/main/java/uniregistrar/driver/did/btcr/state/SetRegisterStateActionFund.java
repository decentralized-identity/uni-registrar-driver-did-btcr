package uniregistrar.driver.did.btcr.state;

import uniregistrar.state.DeactivateState;
import uniregistrar.state.RegisterState;
import uniregistrar.state.UpdateState;

public final class SetRegisterStateActionFund {

    private SetRegisterStateActionFund() {

    }

    public static String getStateActionFundBitcoinAddress(RegisterState registerState) {

        if (!isStateActionFund(registerState)) return null;
        return (String) registerState.getDidState().get("bitcoinAddress");
    }

    public static boolean isStateActionFund(RegisterState registerState) {

        return "fund".equals(SetBtcrRegisterStateAction.getStateAction(registerState));
    }

    public static String getStateActionFundBitcoinAddress(UpdateState updateState) {

        if (!isStateActionFund(updateState)) return null;
        return (String) updateState.getDidState().get("bitcoinAddress");
    }

    public static boolean isStateActionFund(UpdateState updateState) {

        return "fund".equals(SetBtcrRegisterStateAction.getStateAction(updateState));
    }

    public static String getStateActionFundBitcoinAddress(DeactivateState deactivateState) {

        if (!isStateActionFund(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("bitcoinAddress");
    }

    public static boolean isStateActionFund(DeactivateState deactivateState) {

        return "fund".equals(SetBtcrRegisterStateAction.getStateAction(deactivateState));
    }

    public static String getStateActionFundSatoshis(RegisterState registerState) {

        if (!isStateActionFund(registerState)) return null;
        return (String) registerState.getDidState().get("satoshis");
    }

    public static String getStateActionFundSatoshis(UpdateState updateState) {

        if (!isStateActionFund(updateState)) return null;
        return (String) updateState.getDidState().get("satoshis");
    }

    public static String getStateActionFundSatoshis(DeactivateState deactivateState) {

        if (!isStateActionFund(deactivateState)) return null;
        return (String) deactivateState.getDidState().get("satoshis");
    }

    public static void setStateActionFund(RegisterState registerState, String bitcoinAddress, String satoshis) {

        SetBtcrRegisterStateAction.setStateAction(registerState, "fund");
        registerState.getDidState().put("bitcoinAddress", bitcoinAddress);
        registerState.getDidState().put("satoshis", satoshis);
    }

    public static void setStateActionFund(UpdateState updateState, String bitcoinAddress, String satoshis) {

        SetBtcrRegisterStateAction.setStateAction(updateState, "fund");
        updateState.getDidState().put("bitcoinAddress", bitcoinAddress);
        updateState.getDidState().put("satoshis", satoshis);
    }

    public static void setStateActionFund(DeactivateState deactivateState, String bitcoinAddress, String satoshis) {

        SetBtcrRegisterStateAction.setStateAction(deactivateState, "fund");
        deactivateState.getDidState().put("bitcoinAddress", bitcoinAddress);
        deactivateState.getDidState().put("satoshis", satoshis);
    }
}
