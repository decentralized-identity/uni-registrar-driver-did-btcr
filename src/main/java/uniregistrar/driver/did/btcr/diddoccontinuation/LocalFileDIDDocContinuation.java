package uniregistrar.driver.did.btcr.diddoccontinuation;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import foundation.identity.did.DIDDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalFileDIDDocContinuation implements DIDDocContinuation {

	private static final Logger log = LogManager.getLogger(LocalFileDIDDocContinuation.class);

	private String basePath;
	private String baseUri;

	private DIDDocFileNameStrategy didDocFileNameStrategy = new RandomDIDDocFileNameStrategy();

	public LocalFileDIDDocContinuation(String basePath, String baseUri) {

		this.basePath = basePath;
		this.baseUri = baseUri;
	}

	public LocalFileDIDDocContinuation() {
	}

	@Override
	public URI prepareDIDDocContinuation(DIDDocument didContinuationDocument) {

		String fileName = this.didDocFileNameStrategy.createDIDDocFileName(didContinuationDocument);

		String fileUri = this.baseUri;
		if (!fileUri.endsWith("/"))
			fileUri += "/";
		fileUri += fileName;
		if (log.isDebugEnabled())
			log.debug("DID Document continuation URI: " + fileUri);

		return URI.create(fileUri);
	}

	@Override
	public void storeDIDDocContinuation(URI didContinuationUri, DIDDocument didContinuationDocument)
			throws IOException {

		String fileName = didContinuationUri.toString().substring(didContinuationUri.toString().lastIndexOf("/") + 1);

		String filePath = this.basePath;
		if (!filePath.endsWith(File.separator))
			filePath += File.separator;
		filePath += fileName;
		if (log.isDebugEnabled())
			log.debug("DID Document continuation file: " + filePath);

		try (Writer fileWriter = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
			fileWriter.write(didContinuationDocument.toJson());
			fileWriter.flush();
		}
	}

	/*
	 * Getters and setters
	 */

	public String getBasePath() {

		return this.basePath;
	}

	public void setBasePath(String basePath) {

		this.basePath = basePath;
	}

	public String getBaseUri() {

		return this.baseUri;
	}

	public void setBaseUri(String baseUri) {

		this.baseUri = baseUri;
	}

	public DIDDocFileNameStrategy getDidDocFileNameStrategy() {
		return this.didDocFileNameStrategy;
	}

	public void setDidDocFileNameStrategy(DIDDocFileNameStrategy didDocFileNameStrategy) {
		this.didDocFileNameStrategy = didDocFileNameStrategy;
	}
}
