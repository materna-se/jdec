package de.materna.jdec.model;

public class ComplexInputStructure extends InputStructure {
	private Object value;

	public ComplexInputStructure(String type) {
		super(type);
	}

	public ComplexInputStructure(String type, Object value) {
		super(type);

		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
}