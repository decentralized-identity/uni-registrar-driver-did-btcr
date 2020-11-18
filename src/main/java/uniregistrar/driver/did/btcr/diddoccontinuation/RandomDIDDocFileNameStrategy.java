package uniregistrar.driver.did.btcr.diddoccontinuation;

import foundation.identity.did.DIDDocument;

import java.util.UUID;


public class RandomDIDDocFileNameStrategy implements DIDDocFileNameStrategy {

	@Override
	public String createDIDDocFileName(DIDDocument didDocument) {

		return UUID.randomUUID().toString() + ".jsonld";
	}
}
