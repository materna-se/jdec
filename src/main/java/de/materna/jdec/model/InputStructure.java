package de.materna.jdec.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public class InputStructure {
	private String type;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private List<Object> options;

	public InputStructure(String type) {
		this.type = type;
	}

	public InputStructure(String type, List<Object> options) {
		this.type = type;
		this.options = options;
	}

	public String getType() {
		return type;
	}

	public List<Object> getOptions() {
		return options;
	}
}