package de.materna.jdec;

import de.materna.jdec.dmn.DroolsAnalyzer;
import de.materna.jdec.dmn.DroolsDebugger;
import de.materna.jdec.dmn.DroolsHelper;
import de.materna.jdec.dmn.conversions.ActicoConverter;
import de.materna.jdec.dmn.conversions.ConversionResult;
import de.materna.jdec.model.*;
import org.apache.log4j.Logger;
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
import org.kie.dmn.api.core.ast.DecisionServiceNode;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.parser.feel11.profiles.KieExtendedFEELProfile;
import org.kie.dmn.model.api.DMNElementReference;
import org.kie.dmn.model.api.DecisionService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DMNDecisionSession implements DecisionSession {
	private static final Logger log = Logger.getLogger(DMNDecisionSession.class);

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
			compileModels(null);
		}
		catch (ModelImportException ignored) {
		}
	}

	//
	// Store
	//

	@Override
	public List<Model> getModels() {
		return kieRuntime.getModels().stream().map(model -> {
			try {
				return getModel(model.getNamespace());
			}
			catch (ModelNotFoundException ignored) {
			}
			return null; // In theory, this can't happen.
		}).collect(Collectors.toList());
	}

	@Override
	public Model getModel(String namespace) throws ModelNotFoundException {
		DMNModel model = DroolsHelper.getModel(kieRuntime, namespace);
		byte[] source = kieFileSystem.read(getPath(namespace));

		return new Model(
				model.getNamespace(),
				model.getName(),
				new String(source, StandardCharsets.UTF_8),
				model.getDecisions().stream().filter(decisionNode -> decisionNode.getModelNamespace().equals(model.getNamespace())).map(decisionNode -> decisionNode.getName()).collect(Collectors.toSet()),
				model.getInputs().stream().filter(inputDataNode -> inputDataNode.getModelNamespace().equals(model.getNamespace())).map(inputDataNode -> inputDataNode.getName()).collect(Collectors.toSet()),
				model.getBusinessKnowledgeModels().stream().filter(businessKnowledgeModelNode -> businessKnowledgeModelNode.getModelNamespace().equals(model.getNamespace())).map(businessKnowledgeModelNode -> businessKnowledgeModelNode.getName()).collect(Collectors.toSet()),
				model.getDecisionServices().stream().filter(decisionServiceNode -> decisionServiceNode.getModelNamespace().equals(model.getNamespace())).map(decisionServiceNode -> decisionServiceNode.getName()).collect(Collectors.toSet())
		);
	}

	/**
	 * @return Warnings that occurred during compilation.
	 */
	@Override
	public ImportResult importModel(String namespace, String model) throws ModelImportException {
		List<Message> messages = new LinkedList<>();

		//REMOVEME When Actico starts properly exporting models that contain decision services
		ConversionResult conversionResult = ActicoConverter.convertModel(model);
		if (conversionResult.isFixed()) {
			model = conversionResult.getModel();
			messages.add(new Message("The imported model " + namespace + " was exported by ACTICO and was automatically converted to support decision services.", Message.Level.INFO));
		}

		kieFileSystem.write(getPath(namespace), model);

		try {
			return compileModels(messages);
		}
		catch (ModelImportException exception) {
			// Before we can throw the exception, we need to delete the imported model.
			// By doing this, the execution of other models is not affected.
			deleteModel(namespace);

			throw exception;
		}
	}

	@Override
	public void deleteModel(String namespace) throws ModelImportException {
		kieFileSystem.delete(getPath(namespace));
		compileModels(null);
	}

	//
	// Executor
	//


	@Override
	public ExecutionResult executeModel(String namespace, Map<String, Object> inputs) throws ModelNotFoundException {
		return executeModel(DroolsHelper.getModel(kieRuntime, namespace), null, inputs);
	}

	//
	// Analyzer
	//

	@Override
	public Map<String, InputStructure> getInputStructure(String namespace) throws ModelNotFoundException {
		return (Map<String, InputStructure>) DroolsAnalyzer.getComplexInputStructure(kieRuntime, namespace, null).getValue();
	}

	//
	// Overloads
	//

	public ExecutionResult executeModel(DMNModel model, Map<String, ?> inputs) {
		return executeModel(model, null, inputs);
	}

	public ExecutionResult executeModel(String namespace, String decisionServiceName, Map<String, Object> inputs) throws ModelNotFoundException {
		return executeModel(DroolsHelper.getModel(kieRuntime, namespace), decisionServiceName, inputs);
	}

	public Map<String, InputStructure> getInputStructure(String namespace, String decisionServiceName) throws ModelNotFoundException {
		DMNModel model = DroolsHelper.getModel(kieRuntime, namespace);

		Optional<DecisionServiceNode> optionalDecisionServiceNode = model.getDecisionServices().stream().filter(decisionServiceNode -> decisionServiceNode.getName().equals(decisionServiceName)).findFirst();
		if (!optionalDecisionServiceNode.isPresent()) {
			throw new ModelNotFoundException();
		}

		List<DecisionServiceReference> decisionServiceReferences = new LinkedList<>();

		DecisionService decisionService = optionalDecisionServiceNode.get().getDecisionService();
		for (DMNElementReference reference : decisionService.getInputData()) {
			String[] referenceChunks = reference.getHref().split("#");
			decisionServiceReferences.add(new DecisionServiceReference(referenceChunks[0].equals("") ? model.getName() : DroolsHelper.getModel(kieRuntime, referenceChunks[0]).getName(), DecisionServiceReference.DecisionServiceReferenceType.INPUT, referenceChunks[1]));
		}
		for (DMNElementReference reference : decisionService.getInputDecision()) {
			String[] referenceChunks = reference.getHref().split("#");
			decisionServiceReferences.add(new DecisionServiceReference(referenceChunks[0].equals("") ? model.getName() : DroolsHelper.getModel(kieRuntime, referenceChunks[0]).getName(), DecisionServiceReference.DecisionServiceReferenceType.DECISION, referenceChunks[1]));
		}

		return (Map<String, InputStructure>) DroolsAnalyzer.getComplexInputStructure(kieRuntime, namespace, decisionServiceReferences).getValue();
	}

	//
	// Custom Methods
	//

	public ExecutionResult executeModel(DMNModel model, String decisionServiceName, Map<String, ?> inputs) {
		// We need to copy all key-value pairs from the given HashMap<String, Object> into the context
		DMNContext context = kieRuntime.newContext();
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			context.set(entry.getKey(), entry.getValue());
		}

		DroolsDebugger debugger = new DroolsDebugger(this);
		debugger.start(model.getNamespace(), model.getName());
		// By calling evaluateAll, the dmn model and the dmn context are sent to the drools engine
		List<DMNDecisionResult> results = (decisionServiceName == null ? kieRuntime.evaluateAll(model, context) : kieRuntime.evaluateDecisionService(model, context, decisionServiceName)).getDecisionResults();
		debugger.stop();

		// After we've received the results, we need to convert them into a usable format
		Map<String, Object> outputs = new LinkedHashMap<>();
		for (DMNDecisionResult decisionResult : results) {
			// Drools creates a DMNDecisionResult object for all decisions of the model
			// Since not all decisions are executed, we only return the successful ones
			if (decisionResult.getEvaluationStatus() != DMNDecisionResult.DecisionEvaluationStatus.SUCCEEDED) {
				continue;
			}

			outputs.put(decisionResult.getDecisionName(), DroolsHelper.cleanResult(decisionResult.getResult()));
		}

		return new ExecutionResult(outputs, debugger.getDecisions(), debugger.getModelAccessLog(), debugger.getMessages());
	}

	private String getPath(String namespace) {
		return "/src/main/resources/" + namespace + ".dmn";
	}

	/**
	 * Reloads the service by compiling the decision models.
	 *
	 * @param messages Warnings that occurred during compilation.
	 * @return Warnings that occurred during compilation.
	 */
	private ImportResult compileModels(List<Message> messages) throws ModelImportException {
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

			if (messages == null) {
				messages = convertMessages(kieBuilder.getResults().getMessages());
			}
			else {
				messages.addAll(convertMessages(kieBuilder.getResults().getMessages()));
			}

			return new ImportResult(messages);
		}
		catch (Exception exception) {
			try {
				// kieBuilder.getResults() may produce a NullPointerException in org.kie.dmn.core.assembler.DMNAssemblerService.
				if (messages == null) {
					messages = convertMessages(kieBuilder.getResults().getMessages());
				}
				else {
					messages.addAll(convertMessages(kieBuilder.getResults().getMessages()));
				}
			}
			catch (Exception e) {
				if (e.getMessage() == null) {
					messages = Collections.singletonList(new Message("An unknown error has occurred in Drools. Please refer to the logs for further information.", Message.Level.ERROR));
				}
				else {
					messages = Collections.singletonList(new Message(e.getMessage(), Message.Level.ERROR));
				}
			}

			throw new ModelImportException(new ImportResult(messages));
		}
	}

	public DMNRuntime getRuntime() {
		return kieRuntime;
	}

	public ExecutionResult executeExpression(String expression, Map<String, Object> inputs) throws ModelImportException {
		List<FEELProfile> profiles = new ArrayList<>();
		profiles.add(new KieExtendedFEELProfile());
		FEEL feel = FEEL.newInstance(profiles);

		List<Message> messages = new LinkedList<>();
		feel.addListener(feelEvent -> messages.add(new Message(feelEvent.getMessage(), DroolsHelper.convertFEELEventLevel(feelEvent.getSeverity()))));

		HashMap<String, Object> decisions = new LinkedHashMap<>();
		decisions.put("main", DroolsHelper.cleanResult(feel.evaluate(expression, inputs)));

		return new ExecutionResult(decisions, null, messages);
	}

	private List<Message> convertMessages(List<org.kie.api.builder.Message> messages) {
		List<Message> convertedMessages = new LinkedList<>();
		for (org.kie.api.builder.Message message : messages) {
			convertedMessages.add(new Message(message.getText(), Message.Level.valueOf(message.getLevel().name())));
		}
		return convertedMessages;
	}
}