package de.materna.jdec.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModelAccess {
	private ModelAccessType accessType;
	private String name;
	private Map<String, Object> entryContext = new HashMap<>();
	private Map<String, Object> exitContext = new HashMap<>();
	private List<ModelAccess> children = new LinkedList<>();

	public ModelAccess() {
	}

	public ModelAccess(ModelAccessType accessType, String name) {
		this.accessType = accessType;
		this.name = name;
	}

	public ModelAccess(ModelAccessType accessType, String name, Map<String, Object> entryContext) {
		this.accessType = accessType;
		this.name = name;
		this.entryContext = entryContext;
	}

	public ModelAccessType getAccessType() {
		return accessType;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getEntryContext() {
		return entryContext;
	}

	public void setEntryContext(Map<String, Object> entryContext) {
		this.entryContext = entryContext;
	}

	public Map<String, Object> getExitContext() {
		return exitContext;
	}

	public void setExitContext(Map<String, Object> exitContext) {
		this.exitContext = exitContext;
	}

	public List<ModelAccess> getChildren() {
		return children;
	}

	public enum ModelAccessType {
		MODEL,
		DECISION,
		KNOWLEDGE_MODEL
	}
}
