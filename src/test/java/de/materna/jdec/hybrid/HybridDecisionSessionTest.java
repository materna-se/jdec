package de.materna.jdec.hybrid;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.HybridDecisionSession;
import de.materna.jdec.model.ExecutionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HybridDecisionSessionTest {
	@Test
	void executeDMN() throws Exception {
		DecisionSession decisionSession = new HybridDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", decision);

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", inputs);
		Map<String, Object> output = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + output);

		Assertions.assertTrue(output.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", output.get("Employment Status Statement"));
	}

	@Test
	void executeJava() throws Exception {
		DecisionSession decisionSession = new HybridDecisionSession();

		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("EmploymentStatusDecision.java").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		decisionSession.importModel("de.materna.jdec.java.test", "EmploymentStatusDecision", decision);

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		ExecutionResult executionResult = decisionSession.executeModel("de.materna.jdec.java.test", "EmploymentStatusDecision", inputs);
		Map<String, Object> outputs = executionResult.getOutputs();
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeHybrid() throws Exception {
		DecisionSession decisionSession = new HybridDecisionSession();

		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("EmploymentStatusDecision.java").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("de.materna.jdec.java.test", "EmploymentStatusDecision", decision);
		}
		{
			Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
			String decision = new String(Files.readAllBytes(decisionPath));
			decisionSession.importModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", decision);
		}

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		{
			ExecutionResult executionResult = decisionSession.executeModel("de.materna.jdec.java.test", "EmploymentStatusDecision", inputs);
			Map<String, Object> output = executionResult.getOutputs();
			System.out.println("executeHashMap(): " + output);

			Assertions.assertTrue(output.containsKey("Employment Status Statement"));
			Assertions.assertEquals("You are UNEMPLOYED", output.get("Employment Status Statement"));
		}
		{

			ExecutionResult executionResult = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", inputs);
			Map<String, Object> output = executionResult.getOutputs();
			System.out.println("executeHashMap(): " + output);

			Assertions.assertTrue(output.containsKey("Employment Status Statement"));
			Assertions.assertEquals("You are UNEMPLOYED", output.get("Employment Status Statement"));
		}
	}
}