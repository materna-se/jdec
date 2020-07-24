package de.materna.jdec.model;

import java.util.LinkedList;
import java.util.List;

public class ImportResult {
	private List<Message> messages;

	public ImportResult() {
		this.messages = new LinkedList<>();
	}

	public ImportResult(List<Message> messages) {
		this.messages = messages;
	}

	public List<Message> getMessages() {
		return messages;
	}
}
