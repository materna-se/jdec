package de.materna.jdec;

import com.fasterxml.jackson.core.type.TypeReference;
import de.materna.jdec.dmn.DroolsAnalyzer;
import de.materna.jdec.dmn.DroolsDebugger;
import de.materna.jdec.dmn.DroolsHelper;
import de.materna.jdec.dmn.DroolsListener;
import de.materna.jdec.model.*;
import de.materna.jdec.serialization.SerializationHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.ast.DecisionServiceNode;
import org.kie.dmn.core.impl.DMNMessageImpl;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.parser.feel11.profiles.KieExtendedFEELProfile;
import org.kie.dmn.model.api.DMNElementReference;
import org.kie.dmn.model.api.DMNModelInstrumentedBase;
import org.kie.dmn.model.api.DecisionService;
import org.kie.internal.builder.IncrementalResults;
import org.kie.internal.builder.InternalKieBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DMNDecisionSession implements DecisionSession {
	private static final Logger log = LoggerFactory.getLogger(DMNDecisionSession.class);

	private KieServices kieServices;
	public KieFileSystem kieFileSystem;
	private ReleaseId kieReleaseId;
	private KieBuilder kieBuilder;
	private KieSession kieSession;
	private KieContainer kieContainer;
	private DMNRuntime kieRuntime;
	private Set<org.kie.api.builder.Message> kieMessages;

	/**
	 * Creates a KieFileSystem to load decision models dynamically.
	 */
	public DMNDecisionSession() {
		kieServices = KieServices.Factory.get();

		kieFileSystem = kieServices.newKieFileSystem();
		kieReleaseId = kieServices.newReleaseId("org.test", "foo", "1.0-SNAPSHOT");
		kieFileSystem.generateAndWritePomXML(kieReleaseId);

		kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
		kieMessages = new HashSet<>(kieBuilder.getResults().getMessages());

		compileModels();
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
			model.getDecisionServices().stream().filter(decisionServiceNode -> decisionServiceNode.getModelNamespace().equals(model.getNamespace())).map(decisionServiceNode -> decisionServiceNode.getName()).collect(Collectors.toSet()),
			true
		);
	}

	/**
	 * @return Warnings that occurred during compilation.
	 */
	@Override
	public ImportResult importModel(String namespace, String model) throws ModelImportException {
		String path = getPath(namespace);

		// We want to check if the model has changed.
		// To do this, we first read the current model from the virtual file system.
		byte[] currentModel = kieFileSystem.read(path);
		if (currentModel != null) {
			String currentModelString = new String(currentModel, StandardCharsets.UTF_8);
			if (currentModelString.equals(model)) {
				// The model has not changed. We don't need to recompile.
				return new ImportResult(convertMessages(kieMessages));
			}
		}

		kieFileSystem.write(path, model);

		try {
			IncrementalResults results = ((InternalKieBuilder) kieBuilder).incrementalBuild();

			compileModels();

			kieMessages.removeIf(new HashSet<>(results.getRemovedMessages())::contains);
			kieMessages.addAll(results.getAddedMessages());

			List<Message> messages = convertMessages(kieMessages);
			if (messages.stream().anyMatch(message -> message.getLevel() == Message.Level.ERROR)) {
				// Before we can throw the exception, we need to undo the import.

				// We need to reset the kie messages.
				kieMessages.removeIf(new HashSet<>(results.getAddedMessages())::contains);
				kieMessages.addAll(results.getRemovedMessages());

				// We need to delete the model from the file system.
				deleteModel(namespace);

				throw new ModelImportException(new ImportResult(messages));
			}

			return new ImportResult(messages);
		}
		catch (Exception exception) {
			if (exception instanceof ModelImportException) {
				// This is a known exception. We can throw it directly.
				throw (ModelImportException) exception;
			}

			// If we panic, we don't care about the messages from the compilation.
			// We only care about the exception message.
			List<Message> messages;
			if (exception.getMessage() == null) {
				messages = Collections.singletonList(new Message("An unknown error has occurred in Drools. Please refer to the logs for further information.", Message.Level.ERROR));
			}
			else {
				messages = Collections.singletonList(new Message(exception.getMessage(), Message.Level.ERROR));
			}
			throw new ModelImportException(new ImportResult(messages));
		}
	}

	@Override
	public ImportResult deleteModel(String namespace) throws ModelImportException {
		kieFileSystem.delete(getPath(namespace));

		try {
			IncrementalResults results = ((InternalKieBuilder) kieBuilder).incrementalBuild();

			kieMessages.removeIf(new HashSet<>(results.getRemovedMessages())::contains);
			kieMessages.addAll(results.getAddedMessages());

			compileModels();

			List<Message> messages = convertMessages(kieMessages);
			if (messages.stream().anyMatch(message -> message.getLevel() == Message.Level.ERROR)) {
				throw new ModelImportException(new ImportResult(messages));
			}

			return new ImportResult(messages);
		}
		catch (Exception exception) {
			if (exception instanceof ModelImportException) {
				// This is a known exception. We can throw it directly.
				throw (ModelImportException) exception;
			}

			// If we panic, we don't care about the messages from the compilation.
			// We only care about the exception message.
			List<Message> messages;
			if (exception.getMessage() == null) {
				messages = Collections.singletonList(new Message("An unknown error has occurred in Drools. Please refer to the logs for further information.", Message.Level.ERROR));
			}
			else {
				messages = Collections.singletonList(new Message(exception.getMessage(), Message.Level.ERROR));
			}
			throw new ModelImportException(new ImportResult(messages));
		}
	}

	//
	// Executor
	//


	@Override
	public ExecutionResult executeModel(String namespace, Map<String, Object> inputs) throws ModelNotFoundException {
		return executeModel(namespace, inputs, false);
	}

	public ExecutionResult executeModel(String namespace, Map<String, Object> inputs, boolean debug) throws ModelNotFoundException {
		return executeModel(DroolsHelper.getModel(kieRuntime, namespace), null, inputs, debug);
	}

	@Override
	public ExecutionResult executeModel(String namespace, Object input) throws ModelNotFoundException {
		return executeModel(namespace, input, false);
	}

	public ExecutionResult executeModel(String namespace, Object input, boolean debug) throws ModelNotFoundException {
		return executeModel(namespace, SerializationHelper.getInstance().getJSONMapper().convertValue(input, new TypeReference<Map<String, Object>>() {
		}), debug);
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

	public ExecutionResult executeModel(String namespace, String decisionServiceName, Map<String, Object> input) throws ModelNotFoundException {
		return executeModel(namespace, decisionServiceName, input, false);
	}

	public ExecutionResult executeModel(String namespace, String decisionServiceName, Map<String, Object> input, boolean debug) throws ModelNotFoundException {
		return executeModel(DroolsHelper.getModel(kieRuntime, namespace), decisionServiceName, input, debug);
	}

	public ExecutionResult executeModel(String namespace, String decisionServiceName, Object input) throws ModelNotFoundException {
		return executeModel(namespace, decisionServiceName, input, false);
	}

	public ExecutionResult executeModel(String namespace, String decisionServiceName, Object input, boolean debug) throws ModelNotFoundException {
		return executeModel(DroolsHelper.getModel(kieRuntime, namespace), decisionServiceName, SerializationHelper.getInstance().getJSONMapper().convertValue(input, new TypeReference<Map<String, Object>>() {
		}), debug);
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

	private ExecutionResult executeModel(DMNModel model, String decisionServiceName, Map<String, ?> inputs, boolean debug) {
		// We need to copy all key-value pairs from the given HashMap<String, Object> into the context
		DMNContext context = kieRuntime.newContext();
		for (Map.Entry<String, ?> entry : inputs.entrySet()) {
			context.set(entry.getKey(), DroolsHelper.enrichInput(entry.getValue()));
		}

		DroolsListener listener = new DroolsListener(this);
		listener.start(model.getNamespace(), model.getName());

		DroolsDebugger debugger = new DroolsDebugger(this);
		if (debug) {
			debugger.start(model.getNamespace(), model.getName());
		}

		// By calling evaluateAll, the dmn model and the dmn context are sent to the drools engine
		List<DMNDecisionResult> results = (decisionServiceName == null ? kieRuntime.evaluateAll(model, context) : kieRuntime.evaluateDecisionService(model, context, decisionServiceName)).getDecisionResults();
		listener.stop();
		if (debug) {
			debugger.stop();
		}

		// After we've received the results, we need to convert them into a usable format
		Map<String, Object> outputs = new LinkedHashMap<>();
		for (DMNDecisionResult decisionResult : results) {
			// Drools creates a DMNDecisionResult object for all decisions of the model
			// Since not all decisions are executed, we only return the successful ones
			if (decisionResult.getEvaluationStatus() != DMNDecisionResult.DecisionEvaluationStatus.SUCCEEDED) {
				continue;
			}

			outputs.put(decisionResult.getDecisionName(), DroolsHelper.cleanOutput(decisionResult.getResult()));
		}

		return new ExecutionResult(outputs, debugger.getDecisions(), debugger.getModelAccessLog(), listener.getMessages());
	}

	private String getPath(String namespace) {
		return "src/main/resources/" + DigestUtils.md5Hex(namespace).substring(0, 20) + ".dmn";
	}

	/**
	 * Reloads the service by compiling the decision models.
	 *
	 * @return Warnings that occurred during compilation.
	 */
	private void compileModels() {
		// We'll dispose the old session before creating a new one.
		if (kieSession != null) {
			kieSession.dispose();
		}
		if (kieContainer != null) {
			kieContainer.dispose();
		}

		kieContainer = kieServices.newKieContainer(kieReleaseId);

		// KieSession allows the application to establish a connection to the Drools engine.
		// The state is kept across invocations.
		kieSession = kieContainer.newKieSession();

		// Get the KieRuntime through the established connection.
		kieRuntime = kieSession.getKieRuntime(DMNRuntime.class);
		// TODO: Should we enable this? ((DMNRuntimeImpl) kieRuntime).setOption(new RuntimeTypeCheckOption(true));
	}

	public DMNRuntime getRuntime() {
		return kieRuntime;
	}

	public ExecutionResult executeExpression(String expression, Map<String, Object> inputs) throws ModelImportException {
		List<FEELProfile> profiles = new ArrayList<>();
		profiles.add(new KieExtendedFEELProfile());
		FEEL feel = FEEL.newInstance(DMNDecisionSession.class.getClassLoader(), profiles);

		List<Message> messages = new LinkedList<>();
		feel.addListener(feelEvent -> messages.add(new Message(feelEvent.getMessage(), DroolsHelper.convertFEELEventLevel(feelEvent.getSeverity()))));

		HashMap<String, Object> decisions = new LinkedHashMap<>();
		decisions.put("main", DroolsHelper.cleanOutput(feel.evaluate(expression, (Map<String, Object>) DroolsHelper.enrichInput(inputs))));

		return new ExecutionResult(decisions, null, messages);
	}

	private List<Message> convertMessages(Set<org.kie.api.builder.Message> messages) {
		List<Message> convertedMessages = new LinkedList<>();
		for (org.kie.api.builder.Message message : messages) {
			List<String> path = new ArrayList<>();
			/*
			TODO: org.drools.drl.parser.MessageImpl and org.kie.dmn.core.impl.DMNMessageImpl
			try {
				resolvePath(path, (DMNModelInstrumentedBase) ((DMNMessageImpl) message).getSourceReference());
			}
			catch (Exception e) {
				log.error("Error resolving path for message: " + message.getText(), e);
			}
			 */
			convertedMessages.add(new Message(message.getText(), Message.Level.valueOf(message.getLevel().name()), path));
		}
		return convertedMessages;
	}

	private List<String> resolvePath(List<String> path, DMNModelInstrumentedBase source) {
		return path;
	}
}
