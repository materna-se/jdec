package de.materna.jdec;

import de.materna.jdec.dmn.DroolsAnalyzer;
import de.materna.jdec.model.ComplexInputStructure;
import de.materna.jdec.model.ImportResult;
import de.materna.jdec.model.ModelImportException;
import de.materna.jdec.model.ModelNotFoundException;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DMNDecisionSession implements DecisionSession, Closeable {
	public KieFileSystem kieFileSystem;
	private KieServices kieServices;
	private DMNRuntime kieRuntime;

	/**
	 * Creates a KieFileSystem to load decision models dynamically.
	 */
	public DMNDecisionSession() {
		kieServices = KieServices.Factory.get();
		kieFileSystem = kieServices.newKieFileSystem();
		try {
			compileModels();
		}
		catch (ModelImportException ignored) {
		}
	}

	//
	// Store
	//

	@Override
	public String getModel(String namespace, String name) throws ModelNotFoundException {
		byte[] model = kieFileSystem.read(getPath(namespace, name));
		if (model == null) {
			throw new ModelNotFoundException();
		}
		return new String(model);
	}

	@Override
	public ImportResult importModel(String namespace, String name, String model) throws ModelImportException {
		kieFileSystem.write(getPath(namespace, name), model);

		try {
			return compileModels();
		}
		catch (ModelImportException exception) {
			// Before we can throw the exception, we need to delete the imported model.
			// By doing this, the execution of other models is not affected.
			deleteModel(namespace, name);

			throw exception;
		}
	}

	@Override
	public void deleteModel(String namespace, String name) throws ModelImportException {
		kieFileSystem.delete(getPath(namespace, name));
		compileModels();
	}

	//
	// Executor
	//

	@Override
	public Map<String, Object> executeModel(String namespace, String name, Map<String, Object> inputs) throws ModelNotFoundException {
		DMNModel model = kieRuntime.getModel(namespace, name);
		if (model == null) {
			throw new ModelNotFoundException();
		}

		return executeModel(model, inputs);
	}

	//
	// Analyzer
	//

	@Override
	public ComplexInputStructure getInputStructure(String namespace, String name) throws ModelNotFoundException {
		DMNModel model = kieRuntime.getModel(namespace, name);
		if (model == null) {
			throw new ModelNotFoundException();
		}

		return DroolsAnalyzer.getComplexInputStructure(model);
	}

	//
	// Custom Methods
	//

	public Map<String, Object> executeModel(DMNModel model, Map<String, ?> inputs) {
		// We need to copy all key-value pairs from the given HashMap<String, Object> into the context
		DMNContext context = kieRuntime.newContext();
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			context.set(entry.getKey(), entry.getValue());
		}

		// By calling evaluateAll, the dmn model and the dmn context are sent to the drools engine
		List<DMNDecisionResult> results = kieRuntime.evaluateAll(model, context).getDecisionResults();

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

	public void close() {
		kieRuntime = null;
		kieFileSystem = null;
		kieServices = null;
	}

	private String getPath(String namespace, String name) {
		return "/src/main/resources/" + namespace + "/" + name + ".dmn";
	}

	/**
	 * Reloads the service by compiling the decision models.
	 *
	 * @return Warnings that occurred during compilation.
	 */
	private ImportResult compileModels() throws ModelImportException {
		KieBuilder kieBuilder = null;

		try {
			// KieBuilder is a builder for the KieModule
			kieBuilder = kieServices.newKieBuilder(kieFileSystem);

			// KieModule is a container for the resources in the KieContainer
			KieModule kieModule = kieBuilder.getKieModule();

			// KieContainer contains all KieBases of the models in the KieModule
			KieContainer kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());

			// KieSession allows the application to establish a connection to the drools engine
			// The state is kept across invocations
			KieSession kieSession = kieContainer.newKieSession();

			// Get the KieRuntime through the established connection
			kieRuntime = kieSession.getKieRuntime(DMNRuntime.class);

			return new ImportResult(convertMessages(kieBuilder.getResults().getMessages()));
		}
		catch (Exception exception) {
			exception.printStackTrace();

			// noinspection ConstantConditions
			throw new ModelImportException(new ImportResult(convertMessages(kieBuilder.getResults().getMessages())));
		}
	}

	private List<String> convertMessages(List<Message> messages) {
		List<String> convertedMessages = new LinkedList<>();
		for (Message message : messages) {
			convertedMessages.add(message.getText());
		}
		return convertedMessages;
	}

	public DMNRuntime getRuntime() {
		return kieRuntime;
	}

	public ExecutionResult executeExpression(String model, Map<String, Object> inputs) throws ModelImportException {
		List<FEELProfile> profiles = new ArrayList<>();
		profiles.add(new KieExtendedFEELProfile());
		FEEL feel = FEEL.newInstance(profiles);

		List<String> messages = new LinkedList<>();
		feel.addListener(feelEvent -> messages.add(feelEvent.getMessage()));

		HashMap<String, Object> decisions = new LinkedHashMap<>();
		decisions.put("main", DroolsHelper.cleanResult(feel.evaluate(model, inputs)));

		return new ExecutionResult(decisions, null, messages);
	}
}