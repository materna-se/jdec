package de.materna.jdec;

import de.materna.jdec.model.ComplexInputStructure;
import de.materna.jdec.model.ImportResult;

import java.util.Map;

public interface DecisionSession {
	/**
	 * Returns the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	String getModel(String namespace, String name);

	/**
	 * Returns the input structure that is required for the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	ComplexInputStructure getInputStructure(String namespace, String name);

	/**
	 * Imports the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param model     Decision model that will be imported.
	 */
	ImportResult importModel(String namespace, String name, String model);

	/**
	 * Deletes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	void deleteModel(String namespace, String name);

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param inputs    Inputs that will be sent to the execution engine.
	 */
	Map<String, Object> executeModel(String namespace, String name, Map<String, Object> inputs);
}