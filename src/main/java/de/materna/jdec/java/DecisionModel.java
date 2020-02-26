package de.materna.jdec.java;

import de.materna.jdec.model.ComplexModelInput;

import java.util.Map;

public interface DecisionModel {
	/**
	 * Returns the required inputs.
	 */
	ComplexModelInput getInputs();

	/**
	 * Executes the decision model.
	 */
	Map<String, Object> executeDecision(Map<String, ?> inputs);
}
