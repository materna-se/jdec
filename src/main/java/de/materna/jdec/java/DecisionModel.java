package de.materna.jdec.java;

import de.materna.jdec.model.ComplexInputStructure;

import java.util.Map;

public interface DecisionModel {
	/**
	 * Returns the input structure that is required for the decision.
	 */
	ComplexInputStructure getInputStructure();

	/**
	 * Executes the decision.
	 */
	Map<String, Object> executeDecision(Map<String, ?> inputs);
}