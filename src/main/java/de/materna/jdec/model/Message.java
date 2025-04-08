package de.materna.jdec.model;

import java.util.List;

public class Message {
	private String text;
	private Level level;
	private List<String> source;

	public Message(String text, Level level) {
		this.text = text;
		this.level = level;
	}

	public Message(String text, Level level, List<String> source) {
		this.text = text;
		this.level = level;
		this.source = source;
	}

	public String getText() {
		return text;
	}

	public Level getLevel() {
		return level;
	}

	public List<String> getSource() {
		return source;
	}

	public void setSource(List<String> source) {
		this.source = source;
	}

	@Override
	public String toString() {
		return "Message{" + "text='" + text + '\'' + ", level=" + level + ", source=" + source + '}';
	}

	public enum Level {
		INFO,
		WARNING,
		ERROR
	}
}
