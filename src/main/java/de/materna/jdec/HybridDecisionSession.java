package de.materna.jdec;

import de.materna.jdec.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HybridDecisionSession implements DecisionSession {
	private DMNDecisionSession dmnDecisionSession;
	private JavaDecisionSession javaDecisionSession;
	private Map<String, DecisionSessionMapping> decisionSessionMapping;

	public HybridDecisionSession() throws Exception {
		dmnDecisionSession = new DMNDecisionSession();
		javaDecisionSession = new JavaDecisionSession();
		decisionSessionMapping = new LinkedHashMap<>();
	}

	//
	// Store
	//

	@Override
	public List<Model> getModels() {
		return decisionSessionMapping.entrySet().stream().map(entry -> {
			try {
				return getModel(entry.getKey());
			}
			catch (ModelNotFoundException ignored) {
			}
			return null; // In theory, this can't happen.
		}).collect(Collectors.toList());
	}

	@Override
	public Model getModel(String namespace) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace)) {
			case DMN:
				return dmnDecisionSession.getModel(namespace);
			case JAVA:
				return javaDecisionSession.getModel(namespace);
			default:
				throw new ModelNotFoundException();
		}
	}

	@Override
	public ImportResult importModel(String namespace, String model) throws ModelImportException {
		if (model.charAt(0) == '<') {
			ImportResult importResult = dmnDecisionSession.importModel(namespace, model);
			decisionSessionMapping.put(namespace, DecisionSessionMapping.DMN);
			return importResult;
		}

		ImportResult importResult = javaDecisionSession.importModel(namespace, model);
		decisionSessionMapping.put(namespace, DecisionSessionMapping.JAVA);
		return importResult;
	}

	@Override
	public void deleteModel(String namespace) throws ModelImportException {
		if (!decisionSessionMapping.containsKey(namespace)) {
			return;
		}

		switch (decisionSessionMapping.get(namespace)) {
			case DMN:
				dmnDecisionSession.deleteModel(namespace);
			case JAVA:
				javaDecisionSession.deleteModel(namespace);
			default:
				return;
		}
	}

	//
	// Executor
	//

	@Override
	public ExecutionResult executeModel(String namespace, Map<String, Object> inputs) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace)) {
			case DMN:
				return dmnDecisionSession.executeModel(namespace, inputs);
			case JAVA:
				return javaDecisionSession.executeModel(namespace, inputs);
			default:
				throw new ModelNotFoundException();
		}
	}

	//
	// Analyzer
	//

	@Override
	public Map<String, InputStructure> getInputStructure(String namespace) throws ModelNotFoundException {
		if (!decisionSessionMapping.containsKey(namespace)) {
			throw new ModelNotFoundException();
		}

		switch (decisionSessionMapping.get(namespace)) {
			case DMN:
				return dmnDecisionSession.getInputStructure(namespace);
			case JAVA:
				return javaDecisionSession.getInputStructure(namespace);
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

	public DecisionSessionMapping getMapping(String namespace) {
		return decisionSessionMapping.get(namespace);
	}

	private enum DecisionSessionMapping {
		DMN,
		JAVA
	}
}
