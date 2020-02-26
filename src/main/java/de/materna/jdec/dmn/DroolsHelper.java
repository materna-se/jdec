package de.materna.jdec.dmn;

import org.apache.log4j.Logger;
import org.kie.dmn.api.core.DMNUnaryTest;
import org.kie.dmn.core.ast.DMNFunctionDefinitionEvaluator;
import org.kie.dmn.feel.runtime.functions.JavaFunction;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DroolsHelper {
	private static final Logger log = Logger.getLogger(DroolsHelper.class);

	/**
	 * Drools does not return a correctly typed list of allowed values
	 * This method converts the list of allowed values manually
	 *
	 * @param type    Correct type of the allowed values
	 * @param options List of all allowed values
	 */
	public static List<Object> convertOptions(String type, List<DMNUnaryTest> options) {
		List<Object> convertedOptions = new LinkedList<>();
		for (DMNUnaryTest option : options) {
			switch (type) {
				case "string":
				case "date":
				case "time":
				case "dateTime":
					// We need to remove the quotation marks from the allowed value
					convertedOptions.add(option.toString().substring(1, option.toString().length() - 1));
					continue;
				case "number":
					// According to the dmn specification, input ranges like [0..999] could be specified as an allowed value.
					// We'll catch the exception for now.
					try {
						convertedOptions.add(Double.valueOf(option.toString()));
					}
					catch (NumberFormatException ignored) {
					}
					continue;
			}
		}
		return convertedOptions;
	}

	/**
	 * We need to remove all functions because serializing them is not possible.
	 */
	public static Object cleanResult(Object result) {
		if (result instanceof Map) {
			Map<String, Object> results = (Map<String, Object>) result;

			Map<String, Object> cleanedResults = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : results.entrySet()) {
				cleanedResults.put(entry.getKey(), cleanResult(entry.getValue()));
			}
			return cleanedResults;
		}

		if (result instanceof DMNFunctionDefinitionEvaluator.DMNFunction || result instanceof JavaFunction) {
			return "__FUNCTION_DEFINITION__";
		}

		return result;
	}
}