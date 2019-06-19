package de.materna.jdec;

import de.materna.jdec.exceptions.ImportException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class DecisionSessionTest {
	@Test
	void executeHashMap() throws IOException, URISyntaxException {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));

		DecisionSession decisionSession = new DecisionSession();
		decisionSession.importModel(decision);

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		Map<String, Object> outputs = decisionSession.executeModel(inputs);
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeHashMapWithInvalidFEEL() throws IOException, URISyntaxException {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));

		Assertions.assertThrows(ImportException.class, () -> {
			DecisionSession decisionSession = new DecisionSession();
			decisionSession.importModel(decision);
		});
	}
}