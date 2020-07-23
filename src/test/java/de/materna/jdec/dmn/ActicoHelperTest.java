package de.materna.jdec.dmn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ActicoHelperTest {
	@Test
	void fixActicoDecisionServices() throws Exception {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("decision-service.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		String fixedDecision = ActicoHelper.fixActicoDecisionServices(decision);
		Assertions.assertTrue(fixedDecision.contains("xmlns:dmn=\"http://www.omg.org/spec/DMN/20180521/MODEL/\""));
		Assertions.assertTrue(fixedDecision.matches("(?s).*<dmn:variable id=\"(.*?)\" name=\"DecisionService\"/>.*"));
	}

	@Test
	void fixNoActicoDecisionServices() throws Exception {
		Path decisionPath = Paths.get(getClass().getClassLoader().getResource("0085-decision-services.dmn").toURI());
		String decision = new String(Files.readAllBytes(decisionPath));
		Assertions.assertEquals(decision, ActicoHelper.fixActicoDecisionServices(decision));
	}
}
