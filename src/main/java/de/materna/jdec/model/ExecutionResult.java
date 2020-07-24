package de.materna.jdec.model;

import java.util.List;
import java.util.Map;

public class ExecutionResult {
	private Map<String, Object> outputs;
	private Map<String, Map<String, Object>> context;
	private List<Message> messages;

	public ExecutionResult() {
	}

	public ExecutionResult(Map<String, Object> outputs, Map<String, Map<String, Object>> context, List<Message> messages) {
		this.outputs = outputs;
		this.context = context;
		this.messages = messages;
	}

	public Map<String, Object> getOutputs() {
		return outputs;
	}

	public Map<String, Map<String, Object>> getContext() {
		return context;
	}

	public List<Message> getMessages() {
		return messages;
	}
}
