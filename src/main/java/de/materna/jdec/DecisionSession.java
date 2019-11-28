package de.materna.jdec;

import com.fasterxml.jackson.core.type.TypeReference;
import de.materna.jdec.model.ComplexModelInput;
import de.materna.jdec.model.ImportResult;
import de.materna.jdec.drools.DroolsAnalyzer;
import de.materna.jdec.model.ImportException;
import de.materna.jdec.serialization.SerializationHelper;
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

	/**
	 * Creates a KieFileSystem to load decision models dynamically.
	 */
	public DecisionSession() {
		kieServices = KieServices.Factory.get();
		kieFileSystem = kieServices.newKieFileSystem();
	}

	/**
	 * Returns the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	public String getModel(String namespace, String name) {
		return new String(kieFileSystem.read(getPath(name)));
	}

	public ComplexModelInput getInputs(String namespace, String name) {
		return DroolsAnalyzer.getInputs(kieRuntime.getModel(namespace, name));
	}

	/**
	 * Imports the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param model     Decision model that will be imported.
	 */
	public ImportResult importModel(String namespace, String name, String model) {
		kieFileSystem.write(getPath(name), model);

		// If the import fails, we'll delete it again so it doesn't affect other decision models.
		try {
			return reloadService();
		}
		catch (ImportException exception) {
			kieFileSystem.delete(getPath(name));
			return reloadService();
		}
	}

	/**
	 * Deletes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 */
	public void deleteModel(String namespace, String name) {
		kieFileSystem.delete(getPath(name));

		reloadService();
	}

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param inputs    Inputs that will be sent to the execution engine.
	 */
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

	/**
	 * Executes the decision model.
	 *
	 * @param namespace Namespace of the decision model. It can be extracted with /definitions/@namespace.
	 * @param name      Name of the decision model. It can be extracted with /definitions/@name.
	 * @param inputs    Inputs that will be sent to the execution engine.
	 */
	public Map<String, Object> executeModel(String namespace, String name, String inputs) {
		return executeModel(namespace, name, SerializationHelper.getInstance().toClass(inputs, new TypeReference<HashMap<String, Object>>() {
		}));
	}

	private String getPath(String name) {
		return "/src/main/resources/" + name + ".dmn";
	}

	private ImportResult reloadService() throws ImportException {
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