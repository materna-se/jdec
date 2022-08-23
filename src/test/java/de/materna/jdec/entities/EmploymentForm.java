package de.materna.jdec.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmploymentForm {
	@JsonProperty("Employment Status")
	private String employmentStatus;

	public String getEmploymentStatus() {
		return employmentStatus;
	}

	public void setEmploymentStatus(String employmentStatus) {
		this.employmentStatus = employmentStatus;
	}
}
