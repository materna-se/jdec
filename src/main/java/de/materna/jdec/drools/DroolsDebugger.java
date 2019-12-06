package de.materna.jdec.drools;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.model.ModelContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.event.*;
import org.kie.dmn.core.ast.DMNFunctionDefinitionEvaluator;

import java.util.*;

public class DroolsDebugger {
	private DecisionSession decisionSession;

	private List<String> messages = new LinkedList<>();

	private Map<String, Map<String, Object>> decisions = new LinkedHashMap<>();
	private Stack<ModelContext> contexts;

	private DMNRuntimeEventListener listener;

	public DroolsDebugger(DecisionSession decisionSession) {
		this.decisionSession = decisionSession;
	}

	public void start() {
		listener = new DMNRuntimeEventListener() {
			@Override
			public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
				decisions.put(event.getDecision().getName(), new HashMap<>());
				contexts = new Stack<>();
			}

			@Override
			public void beforeEvaluateContextEntry(BeforeEvaluateContextEntryEvent event) {
				String variableName = event.getVariableName();
				if (variableName.equals("__RESULT__")) {
					return;
				}

				// We create a context and put it on the stack.
				// The name allows us to set the value to a higher context level.
				ModelContext context = new ModelContext();
				context.setName(variableName);
				contexts.push(context);
			}

			@Override
			public void afterEvaluateContextEntry(AfterEvaluateContextEntryEvent event) {
				String variableName = event.getVariableName();
				if (variableName.equals("__RESULT__")) {
					return;
				}

				// When we leave the context, we remove it from the stack.
				// If the value has not yet been set by a higher context level, we'll do it.
				// Otherwise, we could overwrite context that we cannot see from this level.
				ModelContext context = contexts.pop();
				if (context.getValue() == null) {
					if (contexts.size() == 0) {
						// If the context has only one level, we need to attach it directly to the decision.
						decisions.get(event.getNodeName()).put(variableName, cleanResult(event.getExpressionResult()));
						return;
					}

					Map<String, Object> value = new HashMap<>();
					value.put(variableName, cleanResult(event.getExpressionResult()));
					context.setValue(value);
				}

				// When we have reached the bottom context, we attach it to the decision.
				if (contexts.size() == 0) {
					decisions.get(event.getNodeName()).put(context.getName(), context.getValue());
					return;
				}

				// If we haven't reached the bottom context, we attach the context to the parent context.
				ModelContext parentContext = contexts.peek();
				if (parentContext.getValue() == null) {
					parentContext.setValue(context.getValue());
					return;
				}
				Map<String, Object> parentContextValue = (Map<String, Object>) parentContext.getValue();
				for (Map.Entry<String, Object> entry : ((Map<String, Object>) context.getValue()).entrySet()) {
					parentContextValue.put(entry.getKey(), entry.getValue());
				}
			}

			@Override
			public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
				for (DMNMessage message : event.getResult().getMessages()) {
					// noinspection deprecation
					messages.add(message.getMessage());
				}
			}
		};
		decisionSession.getRuntime().addListener(listener);
	}

	public void stop() {
		decisionSession.getRuntime().removeListener(listener);
	}

	/**
	 * We need to remove all functions because serializing them is not possible.
	 */
	private Object cleanResult(Object result) {
		if (result instanceof Map) {
			Map<String, Object> results = (Map<String, Object>) result;

			Map<String, Object> cleanedResults = new LinkedHashMap<>();
			for (Map.Entry<String, Object> entry : results.entrySet()) {
				cleanedResults.put(entry.getKey(), cleanResult(entry.getValue()));
			}
			return cleanedResults;
		}

		if (result instanceof DMNFunctionDefinitionEvaluator.DMNFunction) {
			return null;
		}

		return result;
	}

	public Map<String, Map<String, Object>> getDecisions() {
		return decisions;
	}

	public List<String> getMessages() {
		return messages;
	}
}
