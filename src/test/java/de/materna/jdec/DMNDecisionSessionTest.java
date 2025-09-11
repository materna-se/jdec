package de.materna.jdec;

import de.materna.jdec.entities.EmploymentForm;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class DMNDecisionSessionTest {
	@Test
	void executeModel() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("https://github.com/agilepro/dmn-tck");
		InputStructure employmentStatus = inputStructure.get("Employment Status");
		Assertions.assertEquals("string", employmentStatus.getType());
		Assertions.assertEquals(4, employmentStatus.getOptions().size());

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", inputs);

		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void allowedValuesOfReferencedItemDefinitionAreVisible() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("model-SryIqdizos.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://declab.org/SryIqdizos", decision);

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("https://declab.org/SryIqdizos");

		InputStructure rootType = inputStructure.get("Input 1");
		Assertions.assertEquals("array", rootType.getType());
		Assertions.assertEquals(null, rootType.getOptions());

		List<InputStructure> childList = (List<InputStructure>) ((ComplexInputStructure) rootType).getValue();
		Assertions.assertEquals(1, childList.size());

		InputStructure childType = childList.get(0);
		Assertions.assertEquals("string", childType.getType());
		Assertions.assertEquals(2, childType.getOptions().size());
		Assertions.assertEquals(Arrays.asList("a", "b"), childType.getOptions());
	}

	@Test
	void parseItemDefinitionWithChildren() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("model-sxZQRsuYZE.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://declab.org/sxZQRsuYZE", decision);

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("https://declab.org/sxZQRsuYZE");

		InputStructure rootType = inputStructure.get("Input 1");
		Assertions.assertEquals("object", rootType.getType());
		Assertions.assertEquals(null, rootType.getOptions());

		HashMap<String, InputStructure> childMap = (HashMap<String, InputStructure>) ((ComplexInputStructure) rootType).getValue();
		Assertions.assertEquals(2, childMap.size());

		InputStructure firstChildType = childMap.get("Item Component 1");
		Assertions.assertEquals("number", firstChildType.getType());
		Assertions.assertEquals(3, firstChildType.getOptions().size());
		Assertions.assertEquals(Arrays.asList(1.0, 2.0, 3.0), firstChildType.getOptions());

		InputStructure secondChildType = childMap.get("Item Component 2");
		Assertions.assertEquals("array", secondChildType.getType());
		Assertions.assertEquals(null, secondChildType.getOptions());

		List<InputStructure> grandChildList = (List<InputStructure>) ((ComplexInputStructure) secondChildType).getValue();
		Assertions.assertEquals(1, grandChildList.size());

		InputStructure grandChildType = grandChildList.get(0);
		Assertions.assertEquals("string", grandChildType.getType());
		Assertions.assertEquals(null, grandChildType.getOptions());
	}

	@Test
	void executeModelWithSerialization() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, InputStructure> employmentStatus = decisionSession.getInputStructure("https://github.com/agilepro/dmn-tck");

		InputStructure inputStructure = employmentStatus.get("Employment Status");
		Assertions.assertEquals("string", inputStructure.getType());
		Assertions.assertEquals(4, inputStructure.getOptions().size());
		Assertions.assertEquals(Arrays.asList("UNEMPLOYED", "EMPLOYED", "SELF-EMPLOYED", "STUDENT"), inputStructure.getOptions());

		EmploymentForm employmentForm = new EmploymentForm();
		employmentForm.setEmploymentStatus("UNEMPLOYED");
		System.out.println(SerializationHelper.getInstance().toJSON(employmentForm));

		ExecutionResult executionResult = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", employmentForm);

		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeModelFirstWithValidAfterWithInvalidFEEL() throws IOException, URISyntaxException {
		DecisionSession decisionSession = new DMNDecisionSession();

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			ImportResult importResult = decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);

			Assertions.assertEquals(1, decisionSession.getModels().size());
			Assertions.assertEquals(0, importResult.getMessages().size());
		}

		{
			ModelImportException exception = Assertions.assertThrows(ModelImportException.class, () -> {
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
				String decision = new String(Files.readAllBytes(decisionPath));
				decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
			});

			Assertions.assertEquals(0, decisionSession.getModels().size());
			Assertions.assertEquals(1, exception.getResult().getMessages().size());
		}

		{
			ModelImportException exception = Assertions.assertThrows(ModelImportException.class, () -> {
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
				String decision = new String(Files.readAllBytes(decisionPath));
				decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
			});

			Assertions.assertEquals(0, decisionSession.getModels().size());
			Assertions.assertEquals(1, exception.getResult().getMessages().size());
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			ImportResult importResult = decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);

			Assertions.assertEquals(1, decisionSession.getModels().size());
			Assertions.assertEquals(0, importResult.getMessages().size());
		}
	}

	@Test
	void executeModelWithInvalidFEEL() {
		DecisionSession decisionSession = new DMNDecisionSession();

		ModelImportException exception = Assertions.assertThrows(ModelImportException.class, () -> {
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
		});

		Assertions.assertEquals(0, decisionSession.getModels().size());
		List<Message> messages = exception.getResult().getMessages();
		Assertions.assertEquals(1, messages.size());
		Message message = messages.get(0);
		Assertions.assertTrue(message.getText().contains("Error compiling FEEL expression"));
		Assertions.assertEquals(Message.Level.ERROR, message.getLevel());
		List<String> expectedSource = Arrays.asList("TDefinitions", "_0003-input-data-string-allowed-values", "TDecision", "d_EmploymentStatusStatement", "TLiteralExpression", null);
		Assertions.assertEquals(expectedSource, message.getSource());
	}

	@Test
	void executeModelWithInvalidXML() throws IOException, URISyntaxException {
		try {
			DecisionSession decisionSession = new DMNDecisionSession();

			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values-invalid-xml.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
		}
		catch (ModelImportException e) {
			List<Message> messages = e.getResult().getMessages();
			Assertions.assertEquals(1, messages.size());
			Message message = messages.get(0);
			Assertions.assertTrue(message.getText().contains("\"definitions\" is null"));
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
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
				String decision = new String(Files.readAllBytes(decisionPath));
				decisionSession.importModel("https://github.com/agilepro/dmn-tck", decision);
			}

			{
				Path decisionPath = Paths.get(getClass().getClassLoader().getResource("tck/0003-input-data-string-allowed-values.dmn").toURI());
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
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importchildchild.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchildchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importchild.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importparent.dmn").toURI());
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
	void importMultipleModelsWithDecisionsAsInput() throws Exception {
		DMNDecisionSession decisionSession = new DMNDecisionSession();

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importchildchild.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchildchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importchild.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importchild", decision);
		}

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("parent/importparent.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("importparent", decision);
		}

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("importparent", "decisionasinput");
		Assertions.assertTrue(inputStructure.containsKey("ParentInput"));
		Assertions.assertTrue(inputStructure.containsKey("importchild"));
		Assertions.assertTrue(inputStructure.containsKey("importchildchild"));
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
