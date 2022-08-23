package de.materna.jdec;

import com.gs.dmn.DMNModelRepository;
import com.gs.dmn.context.DMNContext;
import com.gs.dmn.context.DMNContextKind;
import com.gs.dmn.context.environment.EnvironmentFactory;
import com.gs.dmn.context.environment.RuntimeEnvironment;
import com.gs.dmn.dialect.StandardDMNDialectDefinition;
import com.gs.dmn.el.analysis.semantics.type.Type;
import com.gs.dmn.el.interpreter.ELInterpreter;
import com.gs.dmn.feel.analysis.semantics.SemanticError;
import com.gs.dmn.runtime.Context;
import com.gs.dmn.runtime.interpreter.Result;
import com.gs.dmn.transformation.InputParameters;
import com.gs.dmn.transformation.basic.BasicDMNToJavaTransformer;
import com.gs.dmn.transformation.lazy.NopLazyEvaluationDetector;
import de.materna.jdec.model.ExecutionResult;
import de.materna.jdec.model.Message;
import de.materna.jdec.model.ModelImportException;

import java.math.BigDecimal;
import java.util.*;

public class GoldmanDecisionSession {
	private InputParameters inputParameters;
	private EnvironmentFactory environmentFactory;
	private BasicDMNToJavaTransformer basicTransformer;
	private StandardDMNDialectDefinition dialectDefinition;
	private DMNModelRepository modelRepository;

	public GoldmanDecisionSession() {
		dialectDefinition = new StandardDMNDialectDefinition();
		modelRepository = new DMNModelRepository();
		inputParameters = new InputParameters(Collections.emptyMap());
		basicTransformer = dialectDefinition.createBasicTransformer(modelRepository, new NopLazyEvaluationDetector(), inputParameters);

		environmentFactory = dialectDefinition.createEnvironmentFactory();
	}

	public ExecutionResult executeExpression(String expression, Map<String, Object> inputs) throws ModelImportException {

		DMNContext context = DMNContext.of(basicTransformer.makeBuiltInContext(), DMNContextKind.GLOBAL, null, environmentFactory.emptyEnvironment(), RuntimeEnvironment.of());
		for (Map.Entry<String, Object> entry : inputs.entrySet()) {
			context.bind(entry.getKey(), convertInput(entry.getValue()));
		}

		ELInterpreter<Type, DMNContext> feelInterpreter = dialectDefinition.createFEELInterpreter(modelRepository, inputParameters);

		try {
			Result result = feelInterpreter.evaluateExpression(expression, context);

			HashMap<String, Object> decisions = new LinkedHashMap<>();
			decisions.put("main", convertOutput(result.getValue()));

			return new ExecutionResult(decisions, null, Collections.emptyList());
		}
		catch (SemanticError e) {
			return new ExecutionResult(Collections.emptyMap(), null, Collections.singletonList(new Message(e.getMessage(), Message.Level.ERROR)));
		}
	}

	private static Object convertInput(Object value) {
		if (value instanceof Map) {
			Map<String, Object> convertedContext = new HashMap<>();

			Map<String, Object> context = (Map<String, Object>) value;
			for (Object key : context.keySet()) {
				convertedContext.put(key.toString(), convertInput(context.get(key)));
			}

			return convertedContext;
		}

		if (value instanceof List) {
			List<Object> convertedContext = new ArrayList<>();

			List<Object> context = (List<Object>) value;
			for (Object entry : context) {
				convertedContext.add(convertInput(entry));
			}

			return convertedContext;
		}

		// It seems that jDMN only supports BigDecimal for numeric values. For this reason, we convert them.
		if (value instanceof Integer) {
			return new BigDecimal((Integer) value);
		}
		if (value instanceof Long) {
			return new BigDecimal((Long) value);
		}
		if (value instanceof Double) {
			return new BigDecimal((Double) value);
		}
		if (value instanceof Float) {
			return new BigDecimal((Float) value);
		}

		return value;
	}

	private static Object convertOutput(Object value) {
		System.out.println(value.getClass().getName());

		if (value instanceof Context) {
			Map<String, Object> convertedContext = new HashMap<>();

			Context context = (Context) value;
			for (Object key : context.keySet()) {
				convertedContext.put(key.toString(), context.get(key));
			}

			return convertedContext;
		}

		return value;
	}
}
