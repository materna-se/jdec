package de.materna.jdec.model;

import java.util.Map;

public class ModelContext {
	private String name;
	private Object value;

	public ModelContext() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}
