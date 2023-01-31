package de.materna.jdec;

import de.materna.jdec.dmn.DroolsHelper;
import de.materna.jdec.model.ExecutionResult;
import de.materna.jdec.model.ImportResult;
import de.materna.jdec.model.Message;
import de.materna.jdec.model.ModelImportException;
import org.camunda.feel.FeelEngine;
import org.camunda.feel.impl.SpiServiceLoader;
import scala.collection.JavaConverters;
import scala.util.Either;

import java.util.*;

public class CamundaDecisionSession {
	private FeelEngine engine;

	public CamundaDecisionSession() {
		engine = new FeelEngine.Builder().valueMapper(SpiServiceLoader.loadValueMapper()).functionProvider(SpiServiceLoader.loadFunctionProvider()).build();
	}

	public ExecutionResult executeExpression(String expression, Map<String, Object> inputs) throws ModelImportException {
		Either<FeelEngine.Failure, Object> failureObjectEither = engine.evalExpression(expression, (Map<String, Object>) DroolsHelper.enrichInput(inputs));
		if (failureObjectEither.isLeft()) {
			throw new ModelImportException(new ImportResult(Collections.singletonList(new Message(failureObjectEither.left().get().message(), Message.Level.ERROR))));
		}

		HashMap<String, Object> decisions = new LinkedHashMap<>();
		decisions.put("main", DroolsHelper.cleanOutput(convertContext(failureObjectEither.right().get())));

		return new ExecutionResult(decisions, null, Collections.emptyList());
	}

	private static Object convertContext(Object value) {
		if (value instanceof scala.collection.Map) {
			Map<String, Object> convertedContext = new HashMap<>();

			Map<String, Object> context = JavaConverters.asJava((scala.collection.Map) value);
			for (Map.Entry<String, Object> entry : context.entrySet()) {
				convertedContext.put(entry.getKey(), convertContext(entry.getValue()));
			}

			return convertedContext;
		}
		if (value instanceof scala.collection.immutable.List) {
			List<Object> convertedContext = new ArrayList<>();

			List<Object> context = JavaConverters.asJava((scala.collection.immutable.List) value);
			for (Object entry : context) {
				convertedContext.add(convertContext(entry));
			}

			return convertedContext;
		}
		return value;
	}
}
