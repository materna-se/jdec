package de.materna.jdec.dmn;

import de.materna.jdec.DMNDecisionSession;
import de.materna.jdec.model.ModelContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.event.*;

import java.util.*;

public class DroolsDebugger {
	private DMNDecisionSession decisionSession;

	private Map<String, Map<String, Object>> decisions = new LinkedHashMap<>();
	private Stack<String> decisionStack = new Stack<>();
	private List<String> messages = new LinkedList<>();
	private Stack<ModelContext> contextStack;

	private DMNRuntimeEventListener listener;

	public DroolsDebugger(DMNDecisionSession decisionSession) {
		this.decisionSession = decisionSession;
	}

	public void start() {
		listener = new DMNRuntimeEventListener() {
			@Override
			public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
				synchronized (decisionSession.getRuntime()) {
					decisionStack.push(event.getDecision().getName());
					decisions.put(decisionStack.peek(), new LinkedHashMap<>());
					contextStack = new Stack<>();
				}
			}

			@Override
			public void beforeEvaluateContextEntry(BeforeEvaluateContextEntryEvent event) {
				synchronized (decisionSession.getRuntime()) {
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
				synchronized (decisionSession.getRuntime()) {
					// When we leave the context, we remove it from the stack.
					// If the value has not yet been set by a higher context level, we'll do it.
					// Otherwise, we could overwrite context that we cannot see from this level.
					ModelContext context = contextStack.pop();
					if (context.getState() == ModelContext.ModelContextState.UNDEFINED) {
						context.setValue(DroolsHelper.cleanResult(event.getExpressionResult()));
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
				synchronized (decisionSession.getRuntime()) {
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

	public Map<String, Map<String, Object>> getDecisions() {
		return decisions;
	}

	public List<String> getMessages() {
		return messages;
	}
}
