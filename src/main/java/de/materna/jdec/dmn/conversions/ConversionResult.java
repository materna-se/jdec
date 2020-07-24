package de.materna.jdec.dmn.conversions;

public class ConversionResult {
	private boolean fixed;
	private String model;

	public ConversionResult(boolean fixed) {
		this.fixed = fixed;
	}

	public ConversionResult(boolean fixed, String model) {
		this.fixed = fixed;
		this.model = model;
	}

	public boolean isFixed() {
		return fixed;
	}

	public String getModel() {
		return model;
	}
}
