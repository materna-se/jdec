package de.materna.jdec.dmn;

import de.materna.jdec.DMNDecisionSession;
import de.materna.jdec.DecisionSession;
import de.materna.jdec.model.ModelImportException;
import de.materna.jdec.model.ModelNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DMNDecisionSessionTest {
	@Test
	void executeHashMap() throws IOException, URISyntaxException {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));

		DecisionSession decisionSession = new DMNDecisionSession();
		decisionSession.importModel("main", "main", decision);

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		Map<String, Object> outputs = decisionSession.executeModel("https://github.com/kiegroup/kie-dmn", "0003-input-data-string-allowed-values", inputs);
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeHashMapWithInvalidFEEL() throws IOException, URISyntaxException {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values-invalid-feel.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));

		Assertions.assertThrows(ModelImportException.class, () -> {
			DecisionSession decisionSession = new DMNDecisionSession();
			decisionSession.importModel("main", "main", decision);
		});
	}

	@Test
	void getModelNotFound() {
		Assertions.assertThrows(ModelNotFoundException.class, () -> {
			DecisionSession decisionSession = new DMNDecisionSession();
			decisionSession.getModel("namespace", "name");
		});
	}
}