package de.materna.jdec.java;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.JavaDecisionSession;
import de.materna.jdec.model.ModelImportException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class JavaDecisionSessionTest {
	@Test
	void executeHashMap() throws Exception {
		DecisionSession decisionSession = new JavaDecisionSession();
		decisionSession.importModel("de.materna.jdec.java.test", "EmploymentStatusDecision", "package de.materna.jdec.java.test;\n" +
				"\n" +
				"import de.materna.jdec.java.DecisionModel;\n" +
				"import de.materna.jdec.model.ComplexInputStructure;\n" +
				"import de.materna.jdec.model.InputStructure;\n" +
				"\n" +
				"import java.util.Arrays;\n" +
				"import java.util.HashMap;\n" +
				"import java.util.LinkedHashMap;\n" +
				"import java.util.Map;\n" +
				"\n" +
				"public class EmploymentStatusDecision extends DecisionModel {\n" +
				"\t@Override\n" +
				"\tpublic ComplexInputStructure getInputStructure() {\n" +
				"\t\tMap<String, InputStructure> inputs = new LinkedHashMap<>();\n" +
				"\n" +
				"\t\tInputStructure inputStructure = new InputStructure(\"string\", Arrays.asList(\"UNEMPLOYED\", \"EMPLOYED\", \"SELF-EMPLOYED\", \"STUDENT\"));\n" +
				"\t\tinputs.put(\"Employment Status\", inputStructure);\n" +
				"\n" +
				"\t\treturn new ComplexInputStructure(\"object\", inputs);\n" +
				"\t}\n" +
				"\n" +
				"\t@Override\n" +
				"\tpublic Map<String, Object> executeDecision(Map<String, Object> inputs) {\n" +
				"\t\tMap<String, Object> output = new HashMap<>();\n" +
				"\t\toutput.put(\"Employment Status Statement\", \"You are \" + inputs.get(\"Employment Status\"));\n" +
				"\t\treturn output;\n" +
				"\t}\n" +
				"}");

		Map<String, Object> inputs = new HashMap<>();
		inputs.put("Employment Status", "UNEMPLOYED");

		Map<String, Object> outputs = decisionSession.executeModel("de.materna.jdec.java.test", "EmploymentStatusDecision", inputs);
		System.out.println("executeHashMap(): " + outputs);

		Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
		Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
	}

	@Test
	void executeHashMapWithMissingMethod() throws Exception {
		DecisionSession decisionSession = new JavaDecisionSession();

		Assertions.assertThrows(ModelImportException.class, () -> {
			decisionSession.importModel("de.materna.jdec.java.test", "EmploymentStatusDecision", "package de.materna.jdec.java.test;\n" +
					"\n" +
					"import de.materna.jdec.java.DecisionModel;\n" +
					"import de.materna.jdec.model.ComplexInputStructure;\n" +
					"import de.materna.jdec.model.InputStructure;\n" +
					"\n" +
					"import java.util.Arrays;\n" +
					"import java.util.HashMap;\n" +
					"import java.util.LinkedHashMap;\n" +
					"import java.util.Map;\n" +
					"\n" +
					"public class EmploymentStatusDecision extends DecisionModel {\n" +
					"\t@Override\n" +
					"\tpublic ComplexInputStructure getInputStructure() {\n" +
					"\t\tMap<String, InputStructure> inputs = new LinkedHashMap<>();\n" +
					"\n" +
					"\t\tInputStructure inputStructure = new InputStructure(\"string\", Arrays.asList(\"UNEMPLOYED\", \"EMPLOYED\", \"SELF-EMPLOYED\", \"STUDENT\"));\n" +
					"\t\tinputs.put(\"Employment Status\", inputStructure);\n" +
					"\n" +
					"\t\treturn new ComplexInputStructure(\"object\", inputs);\n" +
					"\t}\n" +
					"}");
		});
	}
}
