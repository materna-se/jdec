package de.materna.jdec.model;

import java.util.LinkedList;
import java.util.List;

public class ImportResult {
	private List<String> messages;

	public ImportResult() {
		this.messages = new LinkedList<>();
	}

	public ImportResult(List<String> messages) {
		this.messages = messages;
	}

	public List<String> getMessages() {
		return messages;
	}
}
