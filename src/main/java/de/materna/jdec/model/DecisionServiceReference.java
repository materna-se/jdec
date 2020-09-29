package de.materna.jdec.model;

public class DecisionServiceReference {
	private String modelName;
	private DecisionServiceReferenceType entityType;
	private String entityReference;

	public DecisionServiceReference(String modelName, DecisionServiceReferenceType entityType, String entityReference) {
		this.modelName = modelName;
		this.entityType = entityType;
		this.entityReference = entityReference;
	}

	public String getModelName() {
		return modelName;
	}

	public DecisionServiceReferenceType getEntityType() {
		return entityType;
	}

	public String getEntityReference() {
		return entityReference;
	}

	public enum DecisionServiceReferenceType {
		DECISION,
		INPUT
	}
}
