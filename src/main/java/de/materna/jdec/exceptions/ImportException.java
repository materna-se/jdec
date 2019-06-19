package de.materna.jdec.exceptions;

import org.kie.api.builder.Message;

import java.util.List;

public class ImportException extends RuntimeException {
	private List<Message> messages;

	public ImportException() {
	}

	public ImportException(List<Message> messages) {
		this.messages = messages;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
}
