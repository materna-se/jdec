package de.materna.jdec.model;

public class Message {
	private String text;
	private Level level;

	public Message(String text, Level level) {
		this.text = text;
		this.level = level;
	}

	public String getText() {
		return text;
	}

	public Level getLevel() {
		return level;
	}

	@Override
	public String toString() {
		return "Message{" + "text='" + text + '\'' + ", level=" + level + '}';
	}

	public enum Level {
		INFO,
		WARNING,
		ERROR
	}
}
