package de.materna.jdec.model;

public class ImportException extends RuntimeException {
	private ImportResult result;

	public ImportException() {
	}

	public ImportException(ImportResult result) {
		this.result = result;
	}

	public ImportResult getResult() {
		return result;
	}
}
