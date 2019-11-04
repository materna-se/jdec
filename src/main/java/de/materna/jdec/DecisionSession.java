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
import org.kie.dmn.api.core.DMNRuntime;

import java.io.Closeable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecisionSession implements Closeable {
	private KieServices kieServices;
	private KieFileSystem kieFileSystem;
	private KieBuilder kieBuilder;
	private DMNRuntime kieRuntime;

	public DecisionSession() {
		// Get the KieServices instance from the kie ServiceRegistry
		kieServices = KieServices.Factory.get();

		// To load models dynamically, we have to create our own KieFileSystem
		kieFileSystem = kieServices.newKieFileSystem();
	}

	public String readModel(String name) {
		return new String(kieFileSystem.read("/src/main/resources/" + name + ".dmn"));
	}

	public ImportResult importModel(String name, String model) {
		// The path doesn't have to exist, drools temporarily stores the .dmn file at this location
		kieFileSystem.write("/src/main/resources/" + name + ".dmn", model);

		return reloadKie();
	}

	public void deleteModel(String name) {
		kieFileSystem.delete("/src/main/resources/" + name + ".dmn");

		reloadKie();
	}

	public Map<String, Object> executeModel(String namespace, String name, Map<String, ?> inputs) {
		// We need to copy all key-value-pairs from the given HashMap<String, Object> into the context
		DMNContext context = kieRuntime.newContext();
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			context.set(entry.getKey(), entry.getValue());
		}

		// By calling evaluateAll, the dmn model and the dmn context are sent to the drools engine
		List<DMNDecisionResult> results = kieRuntime.evaluateAll(kieRuntime.getModel(namespace, name), context).getDecisionResults();

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

	public Map<String, Object> executeModel(String namespace, String name, String inputs) {
		return executeModel(namespace, name, SerializationHelper.getInstance().toClass(inputs, new TypeReference<HashMap<String, Object>>() {
		}));
	}

	private ImportResult reloadKie() throws ImportException {
		try {
			// KieBuilder is a builder for the KieModule
			kieBuilder = kieServices.newKieBuilder(kieFileSystem);

			// KieModule is a container for the resources in the KieContainer
			KieModule kieModule = kieBuilder.getKieModule();

			// KieContainer contains all KieBases of the models in the KieModule
			KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

			// KieSession allows the application to establish a connection with the drools engine
			// The state is kept across invocations
			KieSession kieSession = kieContainer.newKieSession();

			// Get the KieRuntime through the established connection
			kieRuntime = kieSession.getKieRuntime(DMNRuntime.class);

			return new ImportResult(kieBuilder.getResults().getMessages());
		}
		catch (Exception exception) {
			exception.printStackTrace();

			throw new ImportException(new ImportResult(kieBuilder.getResults().getMessages()));
		}
	}

	public DMNRuntime getRuntime() {
		return kieRuntime;
	}

	public void close() {
		kieRuntime = null;
		kieBuilder = null;
		kieFileSystem = null;
		kieServices = null;
	}
}