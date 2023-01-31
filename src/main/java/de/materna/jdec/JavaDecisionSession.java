package de.materna.jdec;

import com.fasterxml.jackson.core.type.TypeReference;
import de.materna.jdec.java.DecisionModel;
import de.materna.jdec.model.*;
import de.materna.jdec.serialization.SerializationHelper;
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class JavaDecisionSession implements DecisionSession {
	private Compiler compiler;

	private ClassLoader classLoader;
	private ClassLoader parentClassLoader;

	private Map<String, String> models = new LinkedHashMap<>();
	private Map<String, DecisionModel> compiledModels = new LinkedHashMap<>();

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
	public List<Model> getModels() {
		return models.entrySet().stream().map(entry -> {
			try {
				return getModel(entry.getKey());
			}
			catch (ModelNotFoundException ignored) {
			}
			return null; // In theory, this can't happen.
		}).collect(Collectors.toList());
	}

	@Override
	public Model getModel(String namespace) throws ModelNotFoundException {
		String source = models.get(getPath(namespace));
		if (source == null) {
			throw new ModelNotFoundException();
		}

		DecisionModel model = getInstance(namespace);

		String name = model.getClass().getSimpleName();
		return new Model(namespace, name, source, Collections.emptySet(), model.getInputStructure().keySet(), Collections.emptySet(), Collections.emptySet(), true);
	}

	@Override
	public ImportResult importModel(String namespace, String model) throws ModelImportException {
		models.put(getPath(namespace), model);

		try {
			return compileModels();
		}
		catch (ModelImportException exception) {
			// Before we can throw the exception, we need to delete the imported model.
			// By doing this, the execution of other models is not affected.
			deleteModel(namespace);

			throw exception;
		}
	}

	@Override
	public void deleteModel(String namespace) throws ModelImportException {
		models.remove(getPath(namespace));
		compileModels();
	}

	//
	// Executor
	//

	@Override
	public ExecutionResult executeModel(String namespace, Map<String, Object> inputs) throws ModelNotFoundException {
		Map<String, Object> output = getInstance(namespace).executeDecision(inputs);
		return new ExecutionResult(output, Collections.emptyMap(), Collections.emptyList());
	}
	@Override
	public ExecutionResult executeModel(String namespace, Object input) throws ModelNotFoundException {
		return executeModel(namespace, SerializationHelper.getInstance().getJSONMapper().convertValue(input, new TypeReference<Map<String, Object>>() {
		}));
	}

	//
	// Analyzer
	//

	@Override
	public Map<String, InputStructure> getInputStructure(String namespace) throws ModelNotFoundException {
		return getInstance(namespace).getInputStructure();
	}

	//
	// Custom Methods
	//

	private String getPath(String namespace) {
		StringBuilder path = new StringBuilder();

		String[] chunks = namespace.split("\\.");
		for (int i = 0; i < chunks.length; i++) {
			path.append(chunks[i]);
			if (i != chunks.length - 1) { // If it is the last chunk, we need to omit the / at the end.
				path.append('/');
			}
		}

		return path.toString() + ".java";
	}

	private DecisionModel getInstance(String namespace) throws ModelNotFoundException {
		try {
			DecisionModel decisionModel = compiledModels.get(getPath(namespace));
			if (decisionModel == null) {
				decisionModel = (DecisionModel) classLoader.loadClass(namespace).getConstructor().newInstance();

				// The decisionSession is injected into the decisionModel using reflection.
				Field decisionSession = decisionModel.getClass().getSuperclass().getDeclaredField("decisionSession");
				decisionSession.setAccessible(true);
				decisionSession.set(decisionModel, this);
				decisionSession.setAccessible(false);

				compiledModels.put(getPath(namespace), decisionModel);
			}

			return decisionModel;
		}
		catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
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
		Map<String, byte[]> transpiledModels = new LinkedHashMap<>();
		compiler.setClassFileCreator(new MapResourceCreator(transpiledModels));

		List<Message> messages = new LinkedList<>();
		compiler.setWarningHandler((handle, message, location) -> messages.add(new Message(message + (location != null ? (" (Line: " + location.getLineNumber() + ", Column: " + location.getColumnNumber() + ")") : ""), Message.Level.WARNING)));
		compiler.setCompileErrorHandler((message, location) -> messages.add(new Message(message + (location != null ? (" (Line: " + location.getLineNumber() + ", Column: " + location.getColumnNumber() + ")") : ""), Message.Level.ERROR)));

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
