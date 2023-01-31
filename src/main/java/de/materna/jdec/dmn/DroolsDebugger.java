package de.materna.jdec.dmn;

import de.materna.jdec.DMNDecisionSession;
import de.materna.jdec.model.Message;
import de.materna.jdec.model.ModelAccess;
import de.materna.jdec.model.ModelContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.event.*;

import java.util.*;

public class DroolsDebugger {
	private DMNDecisionSession decisionSession;

	private Map<String, Map<String, Object>> decisions = new LinkedHashMap<>();
	private Stack<String> decisionStack = new Stack<>();
	private Stack<ModelContext> contextStack;

	private Stack<ModelAccess> modelAccessLog = new Stack<>();

	private DMNRuntimeEventListener listener;

	public DroolsDebugger(DMNDecisionSession decisionSession) {
		this.decisionSession = decisionSession;
	}

	public void start(String namespace, String name) {
		listener = new DMNRuntimeEventListener() {
			@Override
			public void beforeEvaluateDecisionService(BeforeEvaluateDecisionServiceEvent event) {
				// FIX: If the model contains a decision service is executed, beforeEvaluateAll is not executed.
				synchronized (decisionSession.getRuntime()) {
					modelAccessLog.push(new ModelAccess(ModelAccess.ModelAccessType.MODEL, name, DroolsHelper.cleanContext(event.getResult().getContext().getAll())));
				}
			}

			@Override
			public void beforeEvaluateAll(BeforeEvaluateAllEvent event) {
				synchronized (decisionSession.getRuntime()) {
					modelAccessLog.push(new ModelAccess(ModelAccess.ModelAccessType.MODEL, name, DroolsHelper.cleanContext(event.getResult().getContext().getAll())));
				}
			}

			@Override
			public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
				synchronized (decisionSession.getRuntime()) {
					// If the model name of the evaluated decision does not match the main model name, we need to prefix it.
					String modelName = event.getDecision().getModelName();
					String decisionName = (modelName.equals(name) ? "" : modelName + ".") + event.getDecision().getName();

					decisionStack.push(decisionName);
					decisions.put(decisionStack.peek(), new LinkedHashMap<>());
					contextStack = new Stack<>();

					ModelAccess modelAccess = new ModelAccess(ModelAccess.ModelAccessType.DECISION, decisionName, DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
					modelAccessLog.peek().getChildren().add(modelAccess);
					modelAccessLog.push(modelAccess);
				}
			}

			@Override
			public void beforeInvokeBKM(BeforeInvokeBKMEvent event) {
				synchronized (decisionSession.getRuntime()) {
					// If the model name of the evaluated knowledge model does not match the main model name, we need to prefix it.
					String modelName = event.getBusinessKnowledgeModel().getModelName();
					String knowledgeModelName = (modelName.equals(name) ? "" : modelName + ".") + event.getBusinessKnowledgeModel().getName();

					ModelAccess modelAccess = new ModelAccess(ModelAccess.ModelAccessType.KNOWLEDGE_MODEL, knowledgeModelName, DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
					modelAccessLog.peek().getChildren().add(modelAccess);
					modelAccessLog.push(modelAccess);
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
						context.setValue(DroolsHelper.cleanOutput(event.getExpressionResult()));
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
					decisionStack.pop();

					modelAccessLog.peek().setExitContext(DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
					modelAccessLog.pop();
				}
			}

			@Override
			public void afterInvokeBKM(AfterInvokeBKMEvent event) {
				synchronized (decisionSession.getRuntime()) {
					modelAccessLog.peek().setExitContext(DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
					modelAccessLog.pop();
				}
			}

			@Override
			public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
				synchronized (decisionSession.getRuntime()) {
					modelAccessLog.peek().setExitContext(DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
				}
			}

			@Override
			public void afterEvaluateAll(AfterEvaluateAllEvent event) {
				synchronized (decisionSession.getRuntime()) {
					modelAccessLog.peek().setExitContext(DroolsHelper.cleanContext(event.getResult().getContext().getAll()));
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

	public Stack<ModelAccess> getModelAccessLog() {
		return modelAccessLog;
	}
}
