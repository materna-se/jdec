package de.materna.jdec.dmn;

import de.materna.jdec.DMNDecisionSession;
import de.materna.jdec.model.Message;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.event.*;

import java.util.*;

public class DroolsListener {
	private DMNDecisionSession decisionSession;

	private List<Message> messages = new LinkedList<>();

	private DMNRuntimeEventListener listener;

	public DroolsListener(DMNDecisionSession decisionSession) {
		this.decisionSession = decisionSession;
	}

	public void start(String namespace, String name) {
		listener = new DMNRuntimeEventListener() {
			@Override
			public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
				synchronized (decisionSession.getRuntime()) {
					for (DMNMessage message : event.getResult().getMessages()) {
						messages.add(new Message(message.getMessage(), DroolsHelper.convertMessageLevel(message.getSeverity())));
					}
				}
			}
		};
		decisionSession.getRuntime().addListener(listener);
	}

	public void stop() {
		decisionSession.getRuntime().removeListener(listener);
	}

	public List<Message> getMessages() {
		return messages;
	}
}
