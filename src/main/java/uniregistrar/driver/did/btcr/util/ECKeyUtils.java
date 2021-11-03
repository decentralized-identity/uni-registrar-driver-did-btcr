package uniregistrar.driver.did.btcr.util;

import com.danubetech.keyformats.PrivateKey_to_JWK;
import com.danubetech.keyformats.jose.JWK;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;

public final class ECKeyUtils {

	private static final Logger log = LogManager.getLogger(ECKeyUtils.class);
	public static final String LD_KEY_TYPE = "EcdsaSecp256k1VerificationKey2019";
	private ECKeyUtils() {
	}

	public static JWK privateKeyToJWK(ECKey privateKey) {
		Preconditions.checkNotNull(privateKey, "ECKey cannot be null!");
		return PrivateKey_to_JWK.secp256k1PrivateKey_to_JWK(privateKey, null, null);
	}

	/**
	 * Takes a standard dumped key format and converts it to ECKey
	 * 
	 * @param privWifKey Private key in WiF format
	 * @param chain      Chain to use determine on network parameters
	 * @return Converted EC Key
	 */
	public static ECKey convertWifKeyToECKey(String privWifKey, String chain) {
		Preconditions.checkNotNull(privWifKey, "WiF key cannot be null!");
		Preconditions.checkNotNull(chain, ErrorMessages.CHAIN_IS_NULL);

		log.debug("Converting private WiF Key {}, to ECKey. Chain: {}", () -> privWifKey, () -> chain);
		final ECKey cKey = DumpedPrivateKey.fromBase58(BitcoinUtils.chainToNetworkParameters(chain), privWifKey)
				.getKey();

		log.debug("Converted key: {}", () -> cKey);

		return cKey;
	}

	/**
	 * Generates a new EC Key
	 * 
	 * @return New EC Key
	 */
	public static ECKey getFreshKey() {
		log.debug("Creating a new EC Key...");
		final ECKey fKey = new ECKey();
		log.debug("New EC Key created: {}", fKey::toString);

		return fKey;
	}
}
