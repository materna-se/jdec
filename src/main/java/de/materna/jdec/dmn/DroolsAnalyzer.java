package de.materna.jdec.dmn;

import de.materna.jdec.model.*;
import org.kie.dmn.feel.lang.types.AliasFEELType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.DMNType;
import org.kie.dmn.api.core.ast.DecisionNode;
import org.kie.dmn.api.core.ast.InputDataNode;
import org.kie.dmn.model.api.Import;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DroolsAnalyzer {
	private static final Logger log = LoggerFactory.getLogger(DroolsAnalyzer.class);

	private DroolsAnalyzer() {
	}

	/**
	 * Uses getInputs() to convert all inputs into our own class hierarchy
	 */
	public static ComplexInputStructure getComplexInputStructure(DMNRuntime runtime, String namespace, List<DecisionServiceReference> decisionServiceReferences) throws ModelNotFoundException, ModelIntrospectionException {
		DMNModel model = DroolsHelper.getModel(runtime, namespace);

		ComplexInputStructure modelInput = new ComplexInputStructure("object", false);

		Map<String, InputStructure> inputs = new LinkedHashMap<>();

		if (decisionServiceReferences == null) {
			// We only want to add the inputs that directly belong to the model.
			for (InputDataNode inputDataNode : model.getInputs()) {
				if (inputDataNode.getModelNamespace().equals(namespace)) {
					inputs.put(inputDataNode.getName(), getInputStructure(inputDataNode.getType()));
				}
			}
		}
		else {
			decisionServiceReferences.stream().filter(decisionServiceReference -> decisionServiceReference.getModelName().equals(model.getName())).forEach(decisionServiceReference -> {
				switch (decisionServiceReference.getEntityType()) {
					case DECISION: {
						DecisionNode decisionNode = model.getDecisionById(decisionServiceReference.getEntityReference());
						try {
							inputs.put(decisionNode.getName(), getInputStructure(decisionNode.getResultType()));
						}
						catch (ModelIntrospectionException e) {
							log.error("Could not resolve input structure for decision: {}", decisionNode.getName(), e);
						}
						return;
					}
					case INPUT: {
						InputDataNode inputNode = model.getInputById(decisionServiceReference.getEntityReference());
						try {
							inputs.put(inputNode.getName(), getInputStructure(inputNode.getType()));
						}
						catch (ModelIntrospectionException e) {
							log.error("Could not resolve input structure for input: {}", inputNode.getName(), e);
						}
						return;
					}
				}
			});
		}

		/*
		// All other inputs are resolved and added recursively via the import elements attatched to the definition element.
		for (Import _import : model.getDefinitions().getImport()) {
			inputs.put(_import.getName(), getComplexInputStructure(runtime, _import.getNamespace(), decisionServiceReferences));
		}
		 */

		modelInput.setValue(inputs);

		return modelInput;
	}

	public static InputStructure getInputStructure(DMNType type) throws ModelIntrospectionException {
		return getInputStructure(type, new LinkedHashMap<>());
	}

	private static InputStructure getInputStructure(DMNType type, Map<String, Boolean> currentPath) throws ModelIntrospectionException {
		// cycle detection: if we've already visited this type in the current path, something is wrong.
		String typeKey = type.getNamespace() + "#" + type.getName();
		if (currentPath.containsKey(typeKey)) {
			String cycle = String.join(" -> ", currentPath.keySet()) + " -> " + typeKey;
			throw new ModelIntrospectionException("cycle detected: " + cycle);
		}

		// Mark this type as being processed in the current path.
		currentPath.put(typeKey, true);

		// In order to decide if the input is complex, we get the number of child inputs.
		// If the input contains child inputs, we consider it complex.
		if (!type.getFields().isEmpty()) { // Is it a complex input?
			if (type.isCollection()) { // Is the input a complex collection?
				LinkedList<ComplexInputStructure> inputs = new LinkedList<>();
				inputs.add(new ComplexInputStructure("object", getChildInputStructure(type.getFields(), currentPath)));
				currentPath.remove(typeKey);
				return new ComplexInputStructure("array", inputs);
			}

			InputStructure result = new ComplexInputStructure("object", getChildInputStructure(type.getFields(), currentPath));
			currentPath.remove(typeKey);
			return result;
		}

		// If it's not a complex input, we're recursively resolving the base type and collecting information along the way.

		ResolvedType baseType = getBaseType(null, type, currentPath);

		// Is the input a simple collection?
		// Drools has a quirky way of representing Any, it's also marked as a collection. Let's handle that special case here.
		if (baseType.isCollection() && !baseType.getType().getName().equals("Any")) {
			if (!baseType.getAllowedValues().isEmpty()) { // Is the input a simple collection that contains a list of allowed values?
				LinkedList<InputStructure> inputs = new LinkedList<>();
				inputs.add(new InputStructure(baseType.getType().getName(), baseType.getAllowedValues()));
				return new ComplexInputStructure("array", inputs);
			}

			// The input is a simple collection.
			LinkedList<InputStructure> inputs = new LinkedList<>();
			inputs.add(new InputStructure(baseType.getType().getName()));
			return new ComplexInputStructure("array", inputs);
		}

		if (!baseType.getAllowedValues().isEmpty()) { // Is it a simple input that contains a list of allowed values?
			currentPath.remove(typeKey);
			return new InputStructure(baseType.getType().getName(), baseType.getAllowedValues());
		}

		// The input is as simple as it gets.
		currentPath.remove(typeKey);
		return new InputStructure(baseType.getType().getName());
	}

	/**
	 * Creates a list of all child inputs.
	 * If it contains a complex type, it is resolved by getInput().
	 *
	 * @param fields Child Inputs
	 */
	private static Map<String, InputStructure> getChildInputStructure(Map<String, DMNType> fields, Map<String, Boolean> currentPath) throws ModelIntrospectionException {
		Map<String, InputStructure> inputs = new LinkedHashMap<>();

		for (Map.Entry<String, DMNType> entry : fields.entrySet()) {
			inputs.put(entry.getKey(), getInputStructure(entry.getValue(), currentPath));
		}

		return inputs;
	}

	// The type system is a little strange. Let's assume we have "Item Definition 1", which is a list of "Item Definition 2". "Item Definition 2" is a string and has allowed values.
	// In this case, we want to get back that it is a list of strings with allowed values.
	// To do this, we repeatedly query the underlying type in getBaseType() until we arrive at a (FEEL) base type.
	// However, we need to collect information along the way:
	//   If we find allowed values at any level, we need to keep note of that.
	//   If we see at any level that it is a collection, we need to note that it is a collection.

	/**
	 * Resolves the base type, collecting information about whether it is a collection and any allowed values along the way.
	 * @param resolvedType The resolved type so far. In the beginning, it should be set to null.
	 * @param type The type to resolve.
	 * @param currentPath Map to track types in the current recursion path for cycle detection.
	 * @return The base type.
	 */
	private static ResolvedType getBaseType(ResolvedType resolvedType, DMNType type, Map<String, Boolean> currentPath) {
		if (resolvedType == null) {
			resolvedType = new ResolvedType();
		}

		if (type.isCollection()) {
			resolvedType.isCollection = true;
		}

		if (!type.getAllowedValues().isEmpty()) {
			List<Object> objects = DroolsHelper.convertOptions(type.getAllowedValues());
			resolvedType.allowedValues.addAll(objects);
		}

		if (type.getBaseType() != null) {
			return getBaseType(resolvedType, type.getBaseType(), currentPath);
		}
		else {
			resolvedType.type = type;
			return resolvedType;
		}
	}

	private static class ResolvedType {
		private DMNType type;
		private boolean isCollection = false;
		private List<Object> allowedValues = new LinkedList<>();

		public DMNType getType() {
			return type;
		}

		public void setType(DMNType type) {
			this.type = type;
		}

		public boolean isCollection() {
			return isCollection;
		}

		public void setCollection(boolean collection) {
			isCollection = collection;
		}

		public List<Object> getAllowedValues() {
			return allowedValues;
		}

		public void setAllowedValues(List<Object> allowedValues) {
			this.allowedValues = allowedValues;
		}
	}
}
