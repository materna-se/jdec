package de.materna.jdec.java;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.model.ComplexInputStructure;

import java.util.Map;

public abstract class DecisionModel {
	protected DecisionSession decisionSession;

	/**
	 * Returns the input structure that is required for the decision.
	 */
	public abstract ComplexInputStructure getInputStructure();

	/**
	 * Executes the decision.
	 */
	public abstract Map<String, Object> executeDecision(Map<String, Object> inputs);
}