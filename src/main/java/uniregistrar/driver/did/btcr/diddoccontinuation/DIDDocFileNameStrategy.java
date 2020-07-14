package uniregistrar.driver.did.btcr.diddoccontinuation;

import did.DIDDocument;

public interface DIDDocFileNameStrategy {

	/**
	 *
	 * @param didDocument {@link DIDDocument}
	 * @return created file name for given {@link DIDDocument}
	 */
	String createDIDDocFileName(DIDDocument didDocument);
}
