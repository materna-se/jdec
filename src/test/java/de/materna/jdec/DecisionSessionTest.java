package de.materna.jdec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class DecisionSessionTest {
	private DecisionSession decisionSession;

	@BeforeEach
	void initialize() throws IOException, URISyntaxException {
		// Modell "model.dmn" wird aus dem resources-Ordner geladen.
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0003-input-data-string-allowed-values.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));

		// Modell wird importiert. Bei der Ausführung wird unter anderem die Drools-Instanz initialisiert.
		decisionSession = new DecisionSession();
		decisionSession.importModel(decision);
	}

	/**
	 * Input wird über Map<String, Object> verbalisiert.
	 * executeModel übergibt sie an die Drools-Engine.
	 * Anschließend werden die Ergebnisse in eine Map<String, Object> konvertiert.
	 */
	@Test
	void executeHashMap() {
		// Build the input
		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		// Execute the model to get the output
		Map<String, Object> outputs = decisionSession.executeModel(inputs);
		System.out.println("executeHashMap(): " + outputs);

		// Verify the outputs
		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@AfterEach
	void terminate() throws IOException {
		decisionSession.close();
	}
}