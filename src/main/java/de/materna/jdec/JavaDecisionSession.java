package de.materna.jdec;

import de.materna.jdec.java.DecisionModel;
import de.materna.jdec.model.ComplexInputStructure;
import de.materna.jdec.model.ImportResult;
import de.materna.jdec.model.ModelImportException;
import de.materna.jdec.model.ModelNotFoundException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.Compiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class JavaDecisionSession implements DecisionSession {
	private Compiler compiler;

	private ClassLoader classLoader;
	private ClassLoader parentClassLoader;

	private Map<String, String> models = new HashMap<>();
	private Map<String, DecisionModel> compiledModels = new HashMap<>();

	public JavaDecisionSession() throws Exception {
		parentClassLoader = Thread.currentThread().getContextClassLoader();
		if (parentClassLoader == null) {
			parentClassLoader = this.getClass().getClassLoader(); // TODO: Doesn't that put all classes under the wrong context?
		}

		compiler = (Compiler) CompilerFactoryFactory.getDefaultCompilerFactory().newCompiler();
		compiler.setIClassLoader(new ClassLoaderIClassLoader(parentClassLoader)); // This fixes class loading issues on application servers.
		compileModels();
	}

	//
	// Store
	//

	@Override
	public String getModel(String namespace, String name) throws ModelNotFoundException {
		String model = models.get(getPath(namespace, name));
		if (model == null) {
			throw new ModelNotFoundException();
		}
		return model;
	}

	@Override
	public ImportResult importModel(String namespace, String name, String model) throws ModelImportException {
		models.put(getPath(namespace, name), model);

		try {
			return compileModels();
		}
		catch (ModelImportException exception) {
			// Before we can throw the exception, we need to delete the imported model.
			// By doing this, the execution of other models is not affected.
			deleteModel(namespace, name);

			throw exception;
		}
	}

	@Override
	public void deleteModel(String namespace, String name) throws ModelImportException {
		models.remove(getPath(namespace, name));
		compileModels();
	}

	//
	// Executor
	//

	@Override
	public Map<String, Object> executeModel(String namespace, String name, Map<String, Object> inputs) throws ModelNotFoundException {
		return getInstance(namespace, name).executeDecision(inputs);
	}

	//
	// Analyzer
	//

	@Override
	public ComplexInputStructure getInputStructure(String namespace, String name) throws ModelNotFoundException {
		return getInstance(namespace, name).getInputStructure();
	}

	//
	// Custom Methods
	//

	private String getPath(String namespace, String name) {
		return (namespace == null ? "" : (namespace.replace('.', '/') + "/")) + name + ".java";
	}

	private DecisionModel getInstance(String namespace, String name) throws ModelNotFoundException {
		try {
			DecisionModel decisionModel = compiledModels.get(getPath(namespace, name));
			if (decisionModel == null) {
				decisionModel = (DecisionModel) classLoader.loadClass(namespace + "." + name).getConstructor().newInstance();
				compiledModels.put(getPath(namespace, name), decisionModel);
			}

			return decisionModel;
		}
		catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ModelNotFoundException();
		}
	}

	/**
	 * Reloads the service by transpiling the decision models.
	 *
	 * @return Warnings that occurred during transpilation.
	 */
	private ImportResult compileModels() throws ModelImportException {
		// The compiler needs to be configured before every compilation.
		Map<String, byte[]> transpiledModels = new HashMap<>();
		compiler.setClassFileCreator(new MapResourceCreator(transpiledModels));

		List<String> messages = new LinkedList<>();
		compiler.setWarningHandler((handle, message, location) -> messages.add(message + " (Line: " + location.getLineNumber() + ", Column: " + location.getColumnNumber() + ")"));
		compiler.setCompileErrorHandler((message, location) -> messages.add(message + " (Line: " + location.getLineNumber() + ", Column: " + location.getColumnNumber() + ")"));

		// The compiler is now configured, we can convert all models to resources and compile them afterwards.
		Resource[] resources = new Resource[models.size()];
		Iterator<Map.Entry<String, String>> modelIterator = models.entrySet().iterator();
		for (int i = 0; modelIterator.hasNext(); i++) {
			Map.Entry<String, String> entry = modelIterator.next();
			resources[i] = new StringResource(entry.getKey(), entry.getValue());
		}
		try {
			compiler.compile(resources);
		}
		catch (CompileException | IOException e) {
			throw new ModelImportException(new ImportResult(messages));
		}

		// The compiled models can now be added to a classloader.
		classLoader = new ResourceFinderClassLoader(new MapResourceFinder(transpiledModels), parentClassLoader);

		// Flush the compiled models.
		compiledModels.clear();

		return new ImportResult(messages);
	}
}
