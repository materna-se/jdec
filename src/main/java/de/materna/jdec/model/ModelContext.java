package de.materna.jdec.model;

import java.util.Map;

public class ModelContext {
	private String name;
	private Object value;
	private ModelContextState state;

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

	public ModelContextState getState() {
		return state;
	}

	public void setState(ModelContextState state) {
		this.state = state;
	}

	public enum ModelContextState {
		UNDEFINED,
		VALUE,
		VALUES
	}
}
