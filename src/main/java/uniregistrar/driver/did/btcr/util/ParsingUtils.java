package uniregistrar.driver.did.btcr.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import info.weboftrust.btctxlookup.Chain;
import uniregistrar.driver.did.btcr.DriverConstants;

import static uniregistrar.driver.did.btcr.DriverConstants.*;

public class ParsingUtils {
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final String TIME_ZONE = "UTC";

	private ParsingUtils() {
	}

	public static synchronized String getTimeStamp() {
		df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
		final Timestamp timestamp = Timestamp.from(Instant.now());
		return df.format(timestamp);
	}

	public static synchronized String getTimeStamp(long epochSecond) {
		df.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
		final Timestamp timestamp = Timestamp.from(Instant.ofEpochSecond(epochSecond));
		return df.format(timestamp);
	}

	public static boolean parseBoolean(@Nullable Object object) {
		return parseBoolean(object, DriverConstants.ROTATE_KEY);
	}

	public static boolean parseBoolean(@Nullable Object object, boolean defaultVal) {
		if (object != null) {
			if (object instanceof String) {
				return Boolean.parseBoolean((String) object);
			} else if (object instanceof Boolean) {
				return (boolean) object;
			}
		}
		return defaultVal;
	}

	public static String parseChain(@Nullable Map<String, Object> options) {
		if (MapUtils.isEmpty(options) || StringUtils.isEmpty((String) options.get("chain"))) {
			return DriverConstants.DEFAULT_CHAIN.name();
		} else {
			return Chain.fromString((String) options.get("chain")).name();
		}
	}

	public static String getCertificateString(String givenCertString) {
		if (givenCertString.toUpperCase().contains("CERTIFICATE")) {
			return givenCertString;
		} else {
			return BEGIN_CERT + LINE_SEPARATOR + givenCertString + LINE_SEPARATOR + END_CERT;
		}
	}
}