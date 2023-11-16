package de.materna.jdec.dmn;

import de.materna.jdec.model.Message;
import de.materna.jdec.model.ModelNotFoundException;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.DMNUnaryTest;
import org.kie.dmn.api.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.lang.types.impl.ComparablePeriod;
import org.kie.dmn.feel.runtime.FEELFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.*;

public class DroolsHelper {
	private static final Logger log = LoggerFactory.getLogger(DroolsHelper.class);

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

	public static Object enrichInput(Object result) {
		if (result instanceof Map) {
			Map<String, Object> typedResult = (Map<String, Object>) result;

			Map<String, Object> cleanedResults = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : typedResult.entrySet()) {
				cleanedResults.put(entry.getKey(), enrichInput(entry.getValue()));
			}
			return cleanedResults;
		}

		if (result instanceof List) {
			List<Object> typedResult = (List<Object>) result;
			List<Object> cleanedResults = new ArrayList<>();
			for (Object entry : typedResult) {
				cleanedResults.add(enrichInput(entry));
			}
			return cleanedResults;
		}

		if (result instanceof String) {
			String typedResult = (String) result;
			if (typedResult.startsWith("\uE15A")) {
				return LocalDate.parse(typedResult.substring(1), DateTimeFormatter.ISO_DATE);
			}
			if (typedResult.startsWith("\uE15B")) {
				if (typedResult.contains("+") || typedResult.contains("-") || typedResult.toUpperCase().contains("Z")) {
					return OffsetTime.parse(typedResult.substring(1), DateTimeFormatter.ISO_OFFSET_TIME);
				}
				else {
					return LocalTime.parse(typedResult.substring(1), DateTimeFormatter.ISO_TIME);
				}
			}
			if (typedResult.startsWith("\uE15C")) {
				if (typedResult.contains("+") || typedResult.contains("-") || typedResult.toUpperCase().contains("Z")) {
					return LocalDateTime.parse(typedResult.substring(1), DateTimeFormatter.ISO_DATE_TIME);
				}
				else {
					return ZonedDateTime.parse(typedResult.substring(1), DateTimeFormatter.ISO_ZONED_DATE_TIME);
				}
			}
			if (typedResult.startsWith("\uE15D")) {
				return Duration.parse(typedResult.substring(1));
			}
			if (typedResult.startsWith("\uE15E")) {
				return ComparablePeriod.parse(typedResult.substring(1));
			}
		}

		return result;
	}

	/**
	 * We need to remove all functions because serializing them is not possible.
	 */
	public static Object cleanOutput(Object result) {
		if (result == null) {
			return null;
		}

		if (result instanceof List) {
			List<Object> typedResult = (List<Object>) result;
			List<Object> cleanedResults = new ArrayList<>();
			for (Object entry : typedResult) {
				cleanedResults.add(cleanOutput(entry));
			}
			return cleanedResults;
		}

		if (result instanceof Map) {
			Map<String, Object> typedResult = (Map<String, Object>) result;

			Map<String, Object> cleanedResults = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : typedResult.entrySet()) {
				cleanedResults.put(entry.getKey(), cleanOutput(entry.getValue()));
			}
			return cleanedResults;
		}

		if (result instanceof TemporalAccessor) {
			TemporalAccessor typedResult = (TemporalAccessor) result;

			ZoneId zoneId = typedResult.query(TemporalQueries.zone());
			LocalDate localDate = typedResult.query(TemporalQueries.localDate());
			LocalTime localTime = typedResult.query(TemporalQueries.localTime());
			if (zoneId == null) {
				if (localDate != null && localTime == null) {
					return "\uE15A" + localDate.format(DateTimeFormatter.ISO_DATE);
				}
				if (localDate == null && localTime != null) {
					return "\uE15B" + localTime.format(DateTimeFormatter.ISO_TIME);
				}
				if (localDate != null && localTime != null) {
					return "\uE15C" + LocalDateTime.of(localDate, localTime).format(DateTimeFormatter.ISO_DATE_TIME);
				}
			}
			else {
				if (localDate == null && localTime != null) {
					return "\uE15B" + OffsetTime.of(localTime, zoneId.getRules().getOffset(Instant.now())).format(DateTimeFormatter.ISO_OFFSET_TIME);
				}
				if (localDate != null && localTime == null) {
					return "\uE15C" + ZonedDateTime.of(localDate, LocalTime.of(0, 0), zoneId).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
				}
				if (localDate != null && localTime != null) {
					return "\uE15C" + ZonedDateTime.of(localDate, localTime, zoneId).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
				}
			}
		}
		if (result instanceof Duration) {
			Duration typedResult = (Duration) result;
			return "\uE15D" + typedResult;
		}
		// Drools is using a custom period wrapper, we need to support it as well.
		if (result instanceof ComparablePeriod) {
			ComparablePeriod typedResult = (ComparablePeriod) result;
			return "\uE15E" + typedResult.asPeriod().toString();
		}
		if (result instanceof Period) {
			Period typedResult = (Period) result;
			return "\uE15E" + typedResult;
		}

		if (result instanceof FEELFunction) {
			return "__FUNCTION_DEFINITION__";
		}
		if (result.getClass().getName().startsWith("org.kie.dmn")) {
			return "__INTERNAL__";
		}

		return result;
	}

	/**
	 * The returned context of a decision or knowledge model contains all FEEL functions on the first level.
	 * We need to remove separately before calling cleanResult to not pollute the context.
	 */
	public static Map<String, Object> cleanContext(Map<String, Object> context) {
		Map<String, Object> cleanedInternalContext = new HashMap<>();
		for (Map.Entry<String, Object> entry : context.entrySet()) {
			if (entry.getValue() instanceof FEELFunction) {
				continue;
			}

			cleanedInternalContext.put(entry.getKey(), cleanOutput(entry.getValue()));
		}
		return cleanedInternalContext;
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
