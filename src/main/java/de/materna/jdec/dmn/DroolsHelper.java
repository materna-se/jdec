package de.materna.jdec.dmn;

import de.materna.jdec.model.Message;
import de.materna.jdec.model.ModelNotFoundException;
import org.apache.log4j.Logger;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.DMNUnaryTest;
import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.core.ast.DMNFunctionDefinitionEvaluator;
import org.kie.dmn.feel.runtime.functions.JavaFunction;

import java.util.*;

public class DroolsHelper {
	private static final Logger log = Logger.getLogger(DroolsHelper.class);

	public static DMNModel getModel(DMNRuntime runtime, String namespace) throws ModelNotFoundException {
		Optional<DMNModel> optionalModel = runtime.getModels().stream().filter(model -> model.getNamespace().equals(namespace)).findFirst();
		if (!optionalModel.isPresent()) {
			throw new ModelNotFoundException();
		}

		return optionalModel.get();
	}

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

	public static Message.Level convertMessageLevel(DMNMessage.Severity severity) {
		switch (severity) {
			case TRACE:
			case INFO:
				return Message.Level.INFO;
			case WARN:
				return Message.Level.WARNING;
			default:
				return Message.Level.ERROR;
		}
	}

	public static Message.Level convertFEELEventLevel(FEELEvent.Severity severity) {
		switch (severity) {
			case TRACE:
			case INFO:
				return Message.Level.INFO;
			case WARN:
				return Message.Level.WARNING;
			default:
				return Message.Level.ERROR;
		}
	}
}
