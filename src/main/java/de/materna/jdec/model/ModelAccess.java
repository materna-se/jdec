package de.materna.jdec.model;

import java.util.LinkedList;
import java.util.List;

public class ModelAccess {
	private ModelAccessType accessType;
	private String name;
	private List<ModelAccess> children = new LinkedList<>();

	public ModelAccess(ModelAccessType accessType, String name) {
		this.accessType = accessType;
		this.name = name;
	}

	public ModelAccessType getAccessType() {
		return accessType;
	}

	public String getName() {
		return name;
	}

	public List<ModelAccess> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return "ModelAccess{accessType=" + accessType + ", name='" + name + '\'' + ", children=" + children + '}';
	}

	public enum ModelAccessType {
		MODEL,
		DECISION,
		KNOWLEDGE_MODEL
	}
}
