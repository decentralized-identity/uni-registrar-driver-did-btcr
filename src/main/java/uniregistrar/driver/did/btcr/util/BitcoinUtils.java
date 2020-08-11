package uniregistrar.driver.did.btcr.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Wallet;
import org.bouncycastle.util.encoders.Hex;

import com.google.common.base.Preconditions;

import info.weboftrust.btctxlookup.Chain;

public final class BitcoinUtils {

	private static final Logger log = LogManager.getLogger(BitcoinUtils.class);

	private BitcoinUtils() {
	}

	public static Transaction fromHexToBitcoinJTX(String hexString, Chain chain) {
		return new Transaction(chainToNetworkParameters(chain), Hex.decode(hexString));
	}

	public static NetworkParameters chainToNetworkParameters(Chain chain) {
		if (Chain.REGTESTNET == chain)
			return RegTestParams.get();
		else if (Chain.MAINNET == chain)
			return MainNetParams.get();
		else
			return TestNet3Params.get();
	}

	public static Transaction fromBytesToBitcoinJTX(byte[] payload, Chain chain) {
		return new Transaction(chainToNetworkParameters(chain), payload);
	}

	static String pubKeyToAddress(Chain chain, String pubKey) throws IOException {

		NetworkParameters params = chainToNetworkParameters(chain);
		ECKey eckey;

		try {

			eckey = ECKey.fromPublicOnly(org.apache.commons.codec.binary.Hex.decodeHex(pubKey.toCharArray()));
		} catch (DecoderException ex) {

			throw new IOException("Cannot decode public key " + pubKey + ": " + ex.getMessage(), ex);
		}

		return LegacyAddress.fromPubKeyHash(params, eckey.getPubKeyHash()).toBase58();
	}

	public static Map<String, Object> getBitcoinJWalletSeed(Wallet wallet) throws BitcoinJWalletException {
		DeterministicSeed seed;
		try {
			seed = wallet.getKeyChainSeed();
		} catch (ECKey.MissingPrivateKeyException e) {
			throw new BitcoinJWalletException("Unsupported for the given Wallet!");
		}

		final Long creationTime = seed.getCreationTimeSeconds();
		final List<String> mnemonicCode = seed.getMnemonicCode();

		Map<String, Object> seedAsMap = new LinkedHashMap<>();
		seedAsMap.put("creation_time", creationTime);
		seedAsMap.put("mnemonic_seed", mnemonicCode);
		seedAsMap.put("seed_hex", Hex.toHexString(Preconditions.checkNotNull(seed.getSeedBytes())));
		return seedAsMap;
	}

	public static boolean correctlySpends(TransactionInput txIn, Script scriptPubKey, int inputIndex) {
		try {
			txIn.getScriptSig().correctlySpends(txIn.getParentTransaction(), inputIndex, scriptPubKey,
					Script.ALL_VERIFY_FLAGS);
			return true;
		} catch (ScriptException e) {
			log.error(e);
			return false;
		}
	}

	public static boolean correctlySpends(TransactionInput txIn, Script scriptPubKey, int inputIndex,
			Set<Script.VerifyFlag> flags) {
		try {
			txIn.getScriptSig().correctlySpends(txIn.getParentTransaction(), inputIndex, scriptPubKey, flags);
			return true;
		} catch (ScriptException e) {
			log.error(e);
			return false;
		}
	}

	public static ECKey convertWifKeyToECKey(String privWifKey, String chain) {
		Preconditions.checkNotNull(privWifKey, "WiF key cannot be null!");
		Preconditions.checkNotNull(chain);

		log.debug("Converting private WiF Key {}, to ECKey. Chain: {}", () -> privWifKey, () -> chain);
		final ECKey cKey = DumpedPrivateKey.fromBase58(chainToNetworkParameters(chain), privWifKey).getKey();
		log.debug("Converted key: {}", () -> cKey);

		return cKey;
	}

	public static NetworkParameters chainToNetworkParameters(String chain) {
		if ("REGTESTNET".equalsIgnoreCase(chain))
			return RegTestParams.get();
		else if ("MAINNET".equalsIgnoreCase(chain))
			return MainNetParams.get();
		else if ("TESTNET".equalsIgnoreCase(chain))
			return TestNet3Params.get();
		else {
			throw new IllegalArgumentException("Unknown chain: " + chain);
		}
	}

	public static Address getAddrStringFromKey(Chain chain, ECKey privateKey, Script.ScriptType scriptType) {
		return Address.fromKey(BitcoinUtils.chainToNetworkParameters(chain), privateKey, scriptType);
	}
}
