package de.materna.jdec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.materna.jdec.helpers.SerializationHelper;
import org.junit.jupiter.api.Test;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.feel.lang.ast.BaseNode;
import org.kie.dmn.feel.parser.feel11.ASTBuilderVisitor;
import org.kie.dmn.feel.parser.feel11.FEELParser;
import org.kie.dmn.feel.parser.feel11.FEEL_1_1Parser;
import org.kie.dmn.feel.parser.feel11.profiles.KieExtendedFEELProfile;

import java.io.IOException;
import java.util.*;

public class FEELTest {
	@Test
	void evaluateModel() {
		String input = "(77 - 5) ** 3";
		Map<String, Type> inputTypes = Collections.emptyMap();

		FEEL_1_1Parser parser = FEELParser.parse(null, input, inputTypes, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());

		BaseNode baseNode = new ASTBuilderVisitor(inputTypes).visit(parser.expression());
		System.out.println(baseNode.toString());
	}

	@Test
	void evaluateModel2() {
		List<FEELProfile> profiles = new ArrayList<>();
		profiles.add(new KieExtendedFEELProfile());

		FEEL feel = FEEL.newInstance(profiles);
		Object evaluate = feel.evaluate("first name + last name", new HashMap<String, Object>() {{
			put("first name", "John ");
			put("last name", "Doe");
		}});
		System.out.println(evaluate);
	}

	@Test
	void evaluateModel3() throws IOException {
		ObjectMapper objectMapper = SerializationHelper.getInstance().getObjectMapper();
		JsonNode jsonNode = objectMapper.readTree("[{\"Position\":\"1\",\"Wert\":1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"},{\"Position\":\"2\",\"Wert\":1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"},{\"Position\":\"3\",\"Wert\":1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"}]");
		JsonNode jsonNode2 = objectMapper.readTree("[{\"Position\":\"1\",\"Wert\":0.1E1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"},{\"Position\":\"2\",\"Wert\":1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"},{\"Position\":\"3\",\"Wert\":1,\"Maßeinheit\":\"Liter\",\"Bezeichnung\":\"Zollmenge\"}]");

		System.out.println(jsonNode.equals((expected, calculated) -> {
			if (expected.isNumber() && calculated.isNumber()) {
				return expected.asDouble() == calculated.asDouble() ? 0 : 1;
			}
			System.out.println(expected.toString());
			System.out.println(calculated.toString());
			return 0;
		}, jsonNode2));
	}
}
