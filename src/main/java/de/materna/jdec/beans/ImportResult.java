package de.materna.jdec.beans;

import org.kie.api.builder.Message;

import java.util.LinkedList;
import java.util.List;

public class ImportResult {
	private List<String> messages;

	public ImportResult() {
	}

	public ImportResult(List<Message> messages) {
		List<String> convertedMessages = new LinkedList<>();
		for (Message message : messages) {
			convertedMessages.add(message.getText());
		}
		this.messages = convertedMessages;
	}

	public List<String> getMessages() {
		return messages;
	}
}
