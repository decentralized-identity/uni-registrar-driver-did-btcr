package uniregistrar.driver.did.btcr.diddoccontinuation;

import foundation.identity.did.DIDDocument;

import java.io.IOException;
import java.net.URI;

public interface DIDDocContinuation {

	/**
	 *
	 * @param didDocument {@link DIDDocument} to be stored
	 * @return {@link URI} for given DIDDocuments
	 */
	URI prepareDIDDocContinuation(DIDDocument didDocument);

	/**
	 *
	 * @param didContinuationUri      {@link URI} to use for storing
	 *                                {@link DIDDocument}}
	 *
	 * @param didContinuationDocument DID Continuation Document in form of
	 *                                {@link DIDDocument}
	 * @throws IOException {@link IOException} IOException
	 */
	void storeDIDDocContinuation(URI didContinuationUri, DIDDocument didContinuationDocument) throws IOException;
}
