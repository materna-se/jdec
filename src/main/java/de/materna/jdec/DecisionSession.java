package de.materna.jdec;

import de.materna.jdec.model.*;

import java.util.Map;

public interface DecisionSession {
	/**
	 * Returns the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	String getModel(String namespace, String name) throws ModelNotFoundException;

	/**
	 * Imports the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param model     Decision model that will be imported.
	 */
	ImportResult importModel(String namespace, String name, String model) throws ModelImportException;

	/**
	 * Deletes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	void deleteModel(String namespace, String name) throws ModelImportException;

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param inputs    Inputs that will be sent to the execution engine.
	 */
	ExecutionResult executeModel(String namespace, String name, Map<String, Object> inputs) throws ModelNotFoundException;

	/**
	 * Returns the input structure that is required for executing the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	ComplexInputStructure getInputStructure(String namespace, String name) throws ModelNotFoundException;
}