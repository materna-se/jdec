package de.materna.jdec;

import de.materna.jdec.model.*;

import java.util.List;
import java.util.Map;

public interface DecisionSession {
	/**
	 * Returns information about all decision models.
	 */
	List<Model> getModels();

	/**
	 * Returns information about the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 */
	Model getModel(String namespace) throws ModelNotFoundException;

	/**
	 * Imports the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param model     Decision model that will be imported.
	 */
	ImportResult importModel(String namespace, String model) throws ModelImportException;

	/**
	 * Deletes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 */
	ImportResult deleteModel(String namespace) throws ModelImportException;

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param inputs    Inputs that will be sent to the execution engine.
	 */
	ExecutionResult executeModel(String namespace, Map<String, Object> inputs) throws ModelNotFoundException;

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param input    Input that will be serialized and sent to the execution engine.
	 */
	ExecutionResult executeModel(String namespace, Object input) throws ModelNotFoundException;

	/**
	 * Returns the input structure that is required for executing the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 */
	Map<String, InputStructure> getInputStructure(String namespace) throws ModelNotFoundException;
}
