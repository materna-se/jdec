package de.materna.jdec.java;

import de.materna.jdec.model.ComplexModelInput;

import java.util.Map;

public interface DecisionModel {
	/**
	 * Returns the input that is required for the decision.
	 */
	ComplexModelInput getInput();

	/**
	 * Executes the decision.
	 */
	Map<String, Object> executeDecision(Map<String, ?> inputs);
}