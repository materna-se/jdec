package de.materna.jdec;

import de.materna.jdec.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class JavaDecisionSessionTest {
	@Test
	void getModel() throws Exception {
		JavaDecisionSession decisionSession = new JavaDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("java/EmploymentStatusDecision.java").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		ImportResult importResult = decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);
		Assertions.assertEquals(3, importResult.getMessages().size());

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Model model = decisionSession.getModel("de.materna.jdec.java.test.EmploymentStatusDecision");
		Assertions.assertEquals("de.materna.jdec.java.test.EmploymentStatusDecision", model.getNamespace());
		Assertions.assertEquals("EmploymentStatusDecision", model.getName());
		Assertions.assertEquals("Employment Status", model.getInputs().toArray()[0]);
	}

	@Test
	void getInputStructure() throws Exception {
		JavaDecisionSession decisionSession = new JavaDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("java/EmploymentStatusDecision.java").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		ImportResult importResult = decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);
		Assertions.assertEquals(3, importResult.getMessages().size());

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, InputStructure> inputStructure = decisionSession.getInputStructure("de.materna.jdec.java.test.EmploymentStatusDecision");
		InputStructure employmentStatus = inputStructure.get("Employment Status");
		Assertions.assertEquals("string", employmentStatus.getType());
		Assertions.assertEquals(4, employmentStatus.getOptions().size());
	}

	@Test
	void executeHashMap() throws Exception {
		JavaDecisionSession decisionSession = new JavaDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("java/EmploymentStatusDecision.java").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		ImportResult importResult = decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);
		Assertions.assertEquals(3, importResult.getMessages().size());

		Assertions.assertEquals(1, decisionSession.getModels().size());

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeModel("de.materna.jdec.java.test.EmploymentStatusDecision", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeHashMapWithMissingMethod() throws Exception {
		JavaDecisionSession decisionSession = new JavaDecisionSession();

		Assertions.assertThrows(ModelImportException.class, () -> {
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("java/EmploymentStatusDecision-missing-method.java").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);
		});

		Assertions.assertEquals(0, decisionSession.getModels().size());
	}
}
