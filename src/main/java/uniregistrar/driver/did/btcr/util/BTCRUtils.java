package uniregistrar.driver.did.btcr.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.TimeZone;

import uniregistrar.driver.did.btcr.DriverConfigs;

public final class BTCRUtils {
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private BTCRUtils() {
	}

	public static synchronized String getTimeStamp() {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		final Timestamp timestamp = Timestamp.from(Instant.now());
		return df.format(timestamp);
	}

	/**
	 * @return System specific line separator
	 */
	public static String getLineSeparator() {
		return System.getProperty("line.separator");
	}

	public static String didToPublicKeyDIDURL(String did) {
		return did + "#key-0";
	}

	public static String stripTxref(String txref) {
		return txref.substring(txref.indexOf(':') + 1);
	}

	public static String replaceTxRefHrp(String txref, String chain, DriverConfigs configs) {

		if ("MAINNET".equalsIgnoreCase(chain)) {
			return txref.replaceAll(configs.getMethodPrefix(), configs.getMainnetPrefix());
		} else {
			return txref.replaceAll(configs.getMethodPrefix(), configs.getTestnetPrefix());
		}
	}

	public static String replaceHrpTxRef(String txref, String chain, DriverConfigs configs) {

		if ("MAINNET".equalsIgnoreCase(chain)) {
			return txref.replaceAll(configs.getMethodPrefix(), configs.getMainnetPrefix());
		} else {
			return txref.replaceAll(configs.getMethodPrefix(), configs.getTestnetPrefix());
		}
	}

	public static long estimateWaitingTime(DriverConfigs configs, long lastBlockTime) {
		int reqDepth = configs.getRequiredDepth();
		long currentTime = Instant.now().getEpochSecond();

		// FIXME: Do better calculations
		return Math.max(600, (reqDepth * 600) - (currentTime - lastBlockTime));
	}

}
