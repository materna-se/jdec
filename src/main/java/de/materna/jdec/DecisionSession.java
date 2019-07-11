package de.materna.jdec;

import com.fasterxml.jackson.core.type.TypeReference;
import de.materna.jdec.beans.ImportResult;
import de.materna.jdec.exceptions.ImportException;
import de.materna.jdec.helpers.SerializationHelper;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionSession implements Closeable {
	private DMNRuntime runtime;
	private DMNModel model;

	public ImportResult importModel(String decision) throws ImportException {
		// Get the KieServices instance from the kie ServiceRegistry
		KieServices kieServices = KieServices.Factory.get();

		// To load models dynamically, we have to create our own KieFileSystem
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

		// The path doesn't have to exist, drools temporarily stores the .dmn file at this location
		kieFileSystem.write("/src/main/resources/51a06510-371b-431c-a0f0-dde68937403d.dmn", decision);

		// KieBuilder is a builder for the KieModule
		KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);

		try {
			// KieModule is a container for the resources in the KieContainer
			KieModule kieModule = kieBuilder.getKieModule();

			// KieContainer contains all KieBases of the models in the KieModule
			KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

			// KieSession allows the application to establish a connection with the drools engine
			// The state is kept across invocations
			KieSession kieSession = kieContainer.newKieSession();

			// Get the KieRuntime through the established connection
			runtime = kieSession.getKieRuntime(DMNRuntime.class);
			// Since we've only added one model to the KieFileSystem, we are only interested in the model at index 0
			model = runtime.getModels().get(0);

			return new ImportResult(kieBuilder.getResults().getMessages());
		}
		catch (RuntimeException exception) {
			throw new ImportException(new ImportResult(kieBuilder.getResults().getMessages()));
		}
	}

	public Map<String, Object> executeModel(Map<String, ?> inputs, String... decisions) {
		// We need to copy all key-value-pairs from the given HashMap<String, Object> into the context
		DMNContext context = runtime.newContext();
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			context.set(entry.getKey(), entry.getValue());
		}

		// By calling evaluateAll, the dmn model and the dmn context are sent to the drools engine
		// If decisions are passed, we are executing evaluateByName so the drools engine is filtering the results
		List<DMNDecisionResult> results;
		if (decisions.length == 0) {
			results = runtime.evaluateAll(model, context).getDecisionResults();
		}
		else {
			results = runtime.evaluateByName(model, context, decisions).getDecisionResults();
		}

		// After we've received the results, we need to convert them into a usable format
		Map<String, Object> outputs = new LinkedHashMap<>();
		for (DMNDecisionResult decisionResult : results) {
			// Drools creates a DMNDecisionResult object for all decisions of the model
			// Since not all decisions are executed, we only return the successful ones
			if (decisionResult.getEvaluationStatus() != DMNDecisionResult.DecisionEvaluationStatus.SUCCEEDED) {
				continue;
			}

			outputs.put(decisionResult.getDecisionName(), decisionResult.getResult());
		}

		return outputs;
	}

	public Map<String, Object> executeModel(String inputs, String... decisions) {
		return executeModel(SerializationHelper.getInstance().toClass(inputs, new TypeReference<HashMap<String, Object>>() {
		}), decisions);
	}

	public void close() throws IOException {
		runtime = null;
		model = null;
	}

	public DMNRuntime getRuntime() {
		return runtime;
	}

	public DMNModel getModel() {
		return model;
	}
}