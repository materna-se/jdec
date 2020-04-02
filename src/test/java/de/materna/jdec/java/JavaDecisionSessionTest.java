package de.materna.jdec.java;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.JavaDecisionSession;
import de.materna.jdec.model.ExecutionResult;
import de.materna.jdec.model.ModelImportException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JavaDecisionSessionTest {
	@Test
	void executeHashMap() throws Exception {
		DecisionSession decisionSession = new JavaDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("EmploymentStatusDecision.java").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);

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
		DecisionSession decisionSession = new JavaDecisionSession();

		Assertions.assertThrows(ModelImportException.class, () -> {
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("EmploymentStatusDecision-missing-method.java").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("de.materna.jdec.java.test.EmploymentStatusDecision", decision);
		});
	}
}
