package de.materna.jdec.model;

import java.io.IOException;

public class ModelImportException extends IOException {
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
