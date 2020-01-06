package de.materna.jdec.model;

public class ModelImportException extends RuntimeException {
	private ImportResult result;

	public ModelImportException() {
	}

	public ModelImportException(ImportResult result) {
		this.result = result;
	}

	public ImportResult getResult() {
		return result;
	}
}
