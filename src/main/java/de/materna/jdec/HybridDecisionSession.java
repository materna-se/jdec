package de.materna.jdec;

import de.materna.jdec.model.*;

import java.util.HashMap;
import java.util.Map;

public class HybridDecisionSession implements DecisionSession {
	private DMNDecisionSession dmnDecisionSession;
	private JavaDecisionSession javaDecisionSession;
	private Map<String, DecisionSessionMapping> decisionSessionMapping;

	public HybridDecisionSession() throws Exception {
		dmnDecisionSession = new DMNDecisionSession();
		javaDecisionSession = new JavaDecisionSession();
		decisionSessionMapping = new HashMap<>();
	}

	//
	// Store
	//

	@Override
	public String getModel(String namespace, String name) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace + name)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace + name)) {
			case DMN:
				return dmnDecisionSession.getModel(namespace, name);
			case JAVA:
				return javaDecisionSession.getModel(namespace, name);
			default:
				throw new ModelNotFoundException();
		}
	}

	@Override
	public ImportResult importModel(String namespace, String name, String model) throws ModelImportException {
		if (model.charAt(0) == '<') {
			ImportResult importResult = dmnDecisionSession.importModel(namespace, name, model);
			decisionSessionMapping.put(namespace + name, DecisionSessionMapping.DMN);
			return importResult;
		}

		ImportResult importResult = javaDecisionSession.importModel(namespace, name, model);
		decisionSessionMapping.put(namespace + name, DecisionSessionMapping.JAVA);
		return importResult;
	}

	@Override
	public void deleteModel(String namespace, String name) throws ModelImportException {
		if (!decisionSessionMapping.containsKey(namespace + name)) {
			return;
		}

		switch (decisionSessionMapping.get(namespace + name)) {
			case DMN:
				dmnDecisionSession.deleteModel(namespace, name);
			case JAVA:
				javaDecisionSession.deleteModel(namespace, name);
			default:
				return;
		}
	}

	//
	// Executor
	//

	@Override
	public ExecutionResult executeModel(String namespace, String name, Map<String, Object> inputs) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace + name)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace + name)) {
			case DMN:
				return dmnDecisionSession.executeModel(namespace, name, inputs);
			case JAVA:
				return javaDecisionSession.executeModel(namespace, name, inputs);
			default:
				throw new ModelNotFoundException();
		}
	}

	//
	// Analyzer
	//

	@Override
	public ComplexInputStructure getInputStructure(String namespace, String name) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace + name)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace + name)) {
			case DMN:
				return dmnDecisionSession.getInputStructure(namespace, name);
			case JAVA:
				return javaDecisionSession.getInputStructure(namespace, name);
			default:
				throw new ModelNotFoundException();
		}
	}

	//
	// Custom Methods
	//

	public DMNDecisionSession getDMNDecisionSession() {
		return dmnDecisionSession;
	}

	public JavaDecisionSession getJavaDecisionSession() {
		return javaDecisionSession;
	}

	private enum DecisionSessionMapping {
		DMN,
		JAVA
	}
}
