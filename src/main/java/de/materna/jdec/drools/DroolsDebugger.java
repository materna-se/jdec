package de.materna.jdec.drools;

import de.materna.jdec.DecisionSession;
import de.materna.jdec.model.ModelContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.event.*;
import org.kie.dmn.core.ast.DMNFunctionDefinitionEvaluator;

import java.util.*;

public class DroolsDebugger {
	private DecisionSession decisionSession;

	private Map<String, Map<String, Object>> decisions = new LinkedHashMap<>();
	private Stack<String> decisionStack = new Stack<>();
	private List<String> messages = new LinkedList<>();
	private Stack<ModelContext> contextStack;

	private DMNRuntimeEventListener listener;

	public DroolsDebugger(DecisionSession decisionSession) {
		this.decisionSession = decisionSession;
	}

	public void start() {
		listener = new DMNRuntimeEventListener() {
			@Override
			public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
				synchronized (listener) {
					decisionStack.push(event.getDecision().getName());
					decisions.put(decisionStack.peek(), new LinkedHashMap<>());
					contextStack = new Stack<>();
				}
			}

			@Override
			public void beforeEvaluateContextEntry(BeforeEvaluateContextEntryEvent event) {
				synchronized (listener) {
					// We create a context and put it on the stack.
					// The name allows us to set the value to a higher context level.
					ModelContext context = new ModelContext();
					context.setName(event.getVariableName());
					context.setState(ModelContext.ModelContextState.UNDEFINED);
					contextStack.push(context);
				}
			}

			@Override
			public void afterEvaluateContextEntry(AfterEvaluateContextEntryEvent event) {
				synchronized (listener) {
					// When we leave the context, we remove it from the stack.
					// If the value has not yet been set by a higher context level, we'll do it.
					// Otherwise, we could overwrite context that we cannot see from this level.
					ModelContext context = contextStack.pop();
					if (context.getState() == ModelContext.ModelContextState.UNDEFINED) {
						context.setValue(cleanResult(event.getExpressionResult()));
						context.setState(ModelContext.ModelContextState.VALUE);
					}

					// When we have reached the bottom context, we attach it to the decision.
					if (contextStack.size() == 0) {
						decisions.get(decisionStack.peek()).put(context.getName(), context.getValue());
						return;
					}

					// If we haven't reached the bottom context, we attach the context to the parent context.
					ModelContext parentContext = contextStack.peek();
					// If this is the first value, we'll create a map.
					if (parentContext.getState() == ModelContext.ModelContextState.UNDEFINED) {
						Map<String, Object> value = new LinkedHashMap<>();
						value.put(context.getName(), context.getValue());
						parentContext.setValue(value);
						parentContext.setState(ModelContext.ModelContextState.VALUE);
						return;
					}

					// If there is already a value, we have to check if the key already exists in the map.
					// If it does, we assume that it is a collection and convert it.
					if (parentContext.getState() == ModelContext.ModelContextState.VALUE) {
						Map<String, Object> currentParentContextValue = (Map<String, Object>) parentContext.getValue();
						if (!currentParentContextValue.containsKey(context.getName())) {
							currentParentContextValue.put(context.getName(), context.getValue());
							return;
						}

						List<Map<String, Object>> parentContextValues = new LinkedList<>();

						parentContextValues.add(currentParentContextValue);

						Map<String, Object> newParentContextValue = new LinkedHashMap<>();
						newParentContextValue.put(context.getName(), context.getValue());
						parentContextValues.add(newParentContextValue);

						parentContext.setValue(parentContextValues);
						parentContext.setState(ModelContext.ModelContextState.VALUES);
						return;
					}

					// If there is already a collection, we have to check if the key already exists in the newest element.
					// If it does, we will create a new one.
					List<Map<String, Object>> parentContextValues = (List<Map<String, Object>>) parentContext.getValue();

					Map<String, Object> currentParentContextValue = parentContextValues.get(parentContextValues.size() - 1);
					if (!currentParentContextValue.containsKey(context.getName())) {
						currentParentContextValue.put(context.getName(), context.getValue());
						return;
					}

					Map<String, Object> newParentContextValue = new LinkedHashMap<>();
					newParentContextValue.put(context.getName(), context.getValue());
					parentContextValues.add(newParentContextValue);
				}
			}

			@Override
			public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
				synchronized (listener) {
					for (DMNMessage message : event.getResult().getMessages()) {
						// noinspection deprecation
						messages.add(message.getMessage());
					}

					decisionStack.pop();
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
			return "__FUNCTION_DEFINITION__";
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
