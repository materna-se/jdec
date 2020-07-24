package de.materna.jdec.dmn;

import de.materna.jdec.DMNDecisionSession;
import de.materna.jdec.DecisionSession;
import de.materna.jdec.model.*;
import de.materna.jdec.serialization.SerializationHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMNDecisionSessionTest {
	@Test
	void executeModel() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeModelWithInvalidFEEL() {
		Assertions.assertThrows(ModelImportException.class, () -> {
			DecisionSession decisionSession = new DMNDecisionSession();

			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
		});
	}

	@Test
	void executeModelWithInvalidXML() throws IOException, URISyntaxException {
		try {
			DecisionSession decisionSession = new DMNDecisionSession();

			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values-invalid-xml.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
		}
		catch (ModelImportException e) {
			List<Message> messages = e.getResult().getMessages();
			Assertions.assertEquals(1, messages.size());
			Message message = messages.get(0);
			Assertions.assertTrue(message.getText().contains("An unknown error has occurred in Drools"));
			Assertions.assertEquals(Message.Level.ERROR, message.getLevel());
		}
	}

	@Test
	void getModelNotFound() {
		Assertions.assertThrows(ModelNotFoundException.class, () -> {
			DecisionSession decisionSession = new DMNDecisionSession();
			decisionSession.getModel("namespace");
		});
	}

	@Test
	void importDuplicateModel() throws Exception {
		try {
			DecisionSession decisionSession = new DMNDecisionSession();

			{
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
				String decision = new String(Files.readAllBytes(decisionPath));
				decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
			}

			{
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
				String decision = new String(Files.readAllBytes(decisionPath));
				decisionSession.importModel("https://github.com/agilepro/dmn-tcK", decision);
			}
		}
		catch (ModelImportException e) {
			List<Message> messages = e.getResult().getMessages();
			Assertions.assertEquals(1, messages.size());
			Message message = messages.get(0);
			Assertions.assertTrue(message.getText().contains("Duplicate model name"));
			Assertions.assertEquals(Message.Level.ERROR, message.getLevel());
		}
	}

	@Test
	void importMultipleModels() throws Exception {
		DMNDecisionSession decisionSession = new DMNDecisionSession();

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("import-child-child.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchildchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("import-child.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("import-parent.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importparent", decision);
		}

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("ParentInput", 1);

		Map<String, Object> childInputs = new HashMap<>();
		childInputs.put("ChildInput", 1);

		Map<String, Object> childChildInputs = new HashMap<>();
		childChildInputs.put("ChildChildInput", 1);
		childInputs.put("importchildchild", childChildInputs);
		inputs.put("importchildchild", childChildInputs);

		inputs.put("importchild", childInputs);

		ExecutionResult executionResult = decisionSession.executeModel("importparent", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		Map<String, Map<String, Object>> context = executionResult.getContext();

		Assertions.assertTrue(outputs.containsKey("ParentDecision"));
		Assertions.assertEquals(BigDecimal.valueOf(4), outputs.get("ParentDecision"));

		Assertions.assertTrue(outputs.containsKey("importchild.ChildDecision"));
		Assertions.assertEquals(BigDecimal.valueOf(2), outputs.get("importchild.ChildDecision"));

		Assertions.assertTrue(outputs.containsKey("importchildchild.ChildChildDecision"));
		Assertions.assertEquals(BigDecimal.valueOf(1), outputs.get("importchildchild.ChildChildDecision"));

		Assertions.assertTrue(context.containsKey("ParentDecision"));
		Assertions.assertTrue(context.containsKey("importchild.ChildDecision"));
		Assertions.assertTrue(context.containsKey("importchildchild.ChildChildDecision"));
	}

	@Test
	void executeDecisionService() throws Exception {
		DMNDecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("decision-service.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		ImportResult importResult = decisionSession.importModel("decision-service-actico", decision);
		Assertions.assertEquals(1, importResult.getMessages().size());
		Message message = importResult.getMessages().get(0);
		Assertions.assertTrue(message.getText().contains("automatically converted to support decision services"));
		Assertions.assertEquals(Message.Level.INFO, message.getLevel());

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Input", "test");

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("decision-service-actico", "DecisionService");
		System.out.println(SerializationHelper.getInstance().toJSON(inputStructure));

		ExecutionResult executionResult = decisionSession.executeModel("decision-service-actico", "DecisionService", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		Assertions.assertEquals("{\"PublicDecision\":\"PUBLIC: PRIVATE: test\"}", SerializationHelper.getInstance().toJSON(outputs));
	}

	@Test
	void executeExpression() throws IOException {
		DMNDecisionSession decisionSession = new DMNDecisionSession();

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeExpression("\"You are \" + Employment Status", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("main"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("main"));
	}
}