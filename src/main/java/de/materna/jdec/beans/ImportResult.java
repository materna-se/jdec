package de.materna.jdec.beans;

import org.kie.api.builder.Message;

import java.util.LinkedList;
import java.util.List;

public class ImportResult {
	private boolean successful;
	private List<String> messages;

	public ImportResult() {
	}

	public ImportResult(boolean successful, List<Message> messages) {
		this.successful = successful;

		List<String> convertedMessages = new LinkedList<>();
		for (Message message : messages) {
			convertedMessages.add(message.getText());
		}
		this.messages = convertedMessages;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public List<String> getMessages() {
		return messages;
	}
}
