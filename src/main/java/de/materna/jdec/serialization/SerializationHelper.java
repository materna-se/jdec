package de.materna.jdec.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;

public class SerializationHelper {
	private static SerializationHelper instance;

	private ObjectMapper jsonMapper = new ObjectMapper().registerModules(new ParameterNamesModule(), new JavaTimeModule(), new Jdk8Module()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private ObjectMapper xmlMapper = new XmlMapper().registerModules(new ParameterNamesModule(), new JavaTimeModule(), new Jdk8Module()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private SerializationHelper() {
	}

	public static synchronized SerializationHelper getInstance() {
		if (instance == null) {
			instance = new SerializationHelper();
		}
		return instance;
	}

	public Object toClass(String text, Class<?> clazz) throws RuntimeException {
		try {
			return text.charAt(0) == '<' ? xmlMapper.readValue(text, clazz) : jsonMapper.readValue(text, clazz);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T toClass(String text, TypeReference<T> typeReference) throws RuntimeException {
		try {
			return text.charAt(0) == '<' ? xmlMapper.readValue(text, typeReference) : jsonMapper.readValue(text, typeReference);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String toJSON(Object object) throws RuntimeException {
		try {
			return jsonMapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public String toXML(Object object) throws RuntimeException {
		try {
			return xmlMapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public ObjectMapper getJSONMapper() {
		return jsonMapper;
	}

	public ObjectMapper getXMLMapper() {
		return xmlMapper;
	}
}
