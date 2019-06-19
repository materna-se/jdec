package de.materna.jdec.exceptions;

import de.materna.jdec.beans.ImportResult;
import org.kie.api.builder.Message;

import java.util.List;

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
