package de.materna.jdec.dmn;

import de.materna.jdec.model.ComplexInputStructure;
import de.materna.jdec.model.InputStructure;
import de.materna.jdec.model.ModelNotFoundException;
import org.apache.log4j.Logger;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.api.core.DMNType;
import org.kie.dmn.model.api.Import;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class DroolsAnalyzer {
	private static final Logger log = Logger.getLogger(DroolsAnalyzer.class);

	private DroolsAnalyzer() {
	}

	/**
	 * Uses getInputs() to convert all inputs into our own class hierarchy
	 */
	public static ComplexInputStructure getComplexInputStructure(DMNRuntime runtime, String namespace) throws ModelNotFoundException {
		DMNModel model = DroolsHelper.getModel(runtime, namespace);

		ComplexInputStructure modelInput = new ComplexInputStructure("object", false);

		Map<String, InputStructure> inputs = new LinkedHashMap<>();
		// We only want to add the inputs that directly belong to the model.
		model.getInputs().stream().filter(inputDataNode -> inputDataNode.getModelNamespace().equals(namespace)).forEach(inputDataNode -> {
			inputs.put(inputDataNode.getName(), getInputStructure(inputDataNode.getType()));
		});
		// All other inputs are resolved and added recursively via the import elements attatched to the definition element.
		for (Import _import : model.getDefinitions().getImport()) {
			inputs.put(_import.getNamespace(), getComplexInputStructure(runtime, _import.getNamespace()));
		}
		modelInput.setValue(inputs);

		return modelInput;

	}

	private static InputStructure getInputStructure(DMNType type) {
		// FIX: By explicitly using getBaseType, we avoid the wrong (?) type resolution of the drools engine.
		DMNType baseType = getBaseType(type);

		// In order to decide if the input is complex, we get the number of child inputs.
		// If the input contains child inputs, we consider it complex.
		if (type.getFields().size() != 0) { // Is it a complex input?
			if (type.isCollection()) { // Is the input a complex collection?
				LinkedList<ComplexInputStructure> inputs = new LinkedList<>();
				inputs.add(new ComplexInputStructure("object", getChildInputStructure(type.getFields())));
				return new ComplexInputStructure("array", inputs);
			}

			return new ComplexInputStructure("object", getChildInputStructure(type.getFields()));
		}

		if (type.getAllowedValues().size() != 0) { // Is it a simple input that contains a list of allowed values?
			return new InputStructure(baseType.getName(), DroolsHelper.convertOptions(baseType.getName(), type.getAllowedValues()));
		}

		if (type.isCollection()) { // Is the input a simple collection?
			if (baseType.getAllowedValues().size() != 0) { // Is the input a simple collection that contains a list of allowed values?
				LinkedList<InputStructure> inputs = new LinkedList<>();
				inputs.add(new InputStructure(baseType.getName(), DroolsHelper.convertOptions(baseType.getName(), baseType.getAllowedValues())));
				return new ComplexInputStructure("array", inputs);
			}

			// The input is a simple collection.
			LinkedList<InputStructure> inputs = new LinkedList<>();
			inputs.add(new InputStructure(baseType.getName()));
			return new ComplexInputStructure("array", inputs);
		}

		// The input is as simple as it gets.
		return new InputStructure(baseType.getName());
	}

	/**
	 * Creates a list of all child inputs.
	 * If it contains a complex type, it is resolved by getInput().
	 *
	 * @param fields Child Inputs
	 */
	private static Map<String, InputStructure> getChildInputStructure(Map<String, DMNType> fields) {
		Map<String, InputStructure> inputs = new LinkedHashMap<>();

		for (Map.Entry<String, DMNType> entry : fields.entrySet()) {
			inputs.put(entry.getKey(), getInputStructure(entry.getValue()));
		}

		return inputs;
	}

	private static DMNType getBaseType(DMNType type) {
		if (type.getBaseType() != null) {
			return getBaseType(type.getBaseType());
		}

		return type;
	}
}
