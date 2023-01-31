package de.materna.jdec.dmn.functions;

import com.fasterxml.jackson.core.type.TypeReference;
import de.materna.jdec.serialization.SerializationHelper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.*;

public class FunctionProvider {
	private static CloseableHttpClient httpClient = HttpClientBuilder.create().build();

	public static Object fetch(Map<String, Object> options) {
		// {fetch: function(options) external {java : {class: "de.materna.jdec.dmn.functions.FunctionProvider", method signature: "fetch(java.util.Map)"}}}.fetch({url: "http://httpbin.org/anything", method: "POST", headers: {test: "jax"}, body: {"nein": true}})
		try {
			HttpEntityEnclosingRequestBase httpRequest = new HttpEntityEnclosingRequestBase() {
				@Override
				public String getMethod() {
					return (String) options.getOrDefault("method", "GET");
				}

				@Override
				public URI getURI() {
					return URI.create((String) options.get("url"));
				}
			};

			if (options.containsKey("headers")) {
				Map<String, String> headers = (Map<String, String>) options.get("headers");
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					httpRequest.setHeader(entry.getKey(), entry.getValue());
				}
			}

			if (options.containsKey("body")) {
				Object body = options.get("body");
				httpRequest.setEntity(new StringEntity(body instanceof String ? (String) body : SerializationHelper.getInstance().toJSON(body)));
			}

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
				HashMap<String, Object> response = new HashMap<>();

				response.put("status", httpResponse.getStatusLine().getStatusCode());

				Map<String, String> headers = new HashMap<>();
				for (Header header : httpResponse.getAllHeaders()) {
					headers.put(header.getName(), header.getValue());
				}
				response.put("headers", headers);

				String body = EntityUtils.toString(httpResponse.getEntity());
				try {
					response.put("body", SerializationHelper.getInstance().toClass(body, Object.class));
				}
				catch (RuntimeException e) {
					response.put("body", body);
				}

				return response;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Map<String, Object> mergeContexts(List<Map<String, Object>> contexts) {
		Map<String, Object> result = new HashMap<>();
		for (Map<String, Object> context : contexts) {
			result.putAll(context);
		}
		return result;
	}

	public static List<Object> resolvePath(Object context, String path) {
		try {
			String[] splitPath = path.substring(1).split("/");
			return resolve(context, Collections.emptyList(), Collections.emptyList(), Arrays.asList(splitPath));
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static String stringifyPath(List<String> path) {
		return "/" + String.join("/", path);
	}

	private static Map<String, Object> createPathMatch(String path, Object value) {
		Map<String, Object> result = new HashMap<>();
		result.put("path", path);
		result.put("value", value);
		return result;
	}

	private static List<Object> resolve(Object context, List<Object> traversedContexts, List<String> traversedChunks, List<String> remainingChunks) {
		String currentChunk = remainingChunks.get(0);

		if(currentChunk.equals("..")) {
			List<Object> result = new ArrayList<>();

			Object previousContext = traversedContexts.get(traversedContexts.size() - 1);

			List<Object> newTraversedContexts = new ArrayList<>(traversedContexts);
			newTraversedContexts.remove(newTraversedContexts.size() - 1);

			List<String> newTraversedChunks = new ArrayList<>(traversedChunks);
			newTraversedChunks.remove(newTraversedChunks.size() - 1);

			if (remainingChunks.size() == 1) {
				result.add(createPathMatch(stringifyPath(newTraversedChunks), previousContext));
			}
			else {
				result.addAll(resolve(previousContext, newTraversedContexts, newTraversedChunks, remainingChunks.subList(1, remainingChunks.size())));
			}

			return result;
		}

		if (context instanceof Map) {
			List<Object> result = new ArrayList<>();

			Map<String, Object> map = (Map<String, Object>) context;
			if (!map.containsKey(currentChunk)) {
				return Collections.emptyList();
			}
			Object value = map.get(currentChunk);

			List<Object> newTraversedContexts = new ArrayList<>(traversedContexts);
			newTraversedContexts.add(context);

			List<String> newTraversedChunks = new ArrayList<>(traversedChunks);
			newTraversedChunks.add(currentChunk);

			if (remainingChunks.size() == 1) {
				result.add(createPathMatch(stringifyPath(newTraversedChunks), value));
			}
			else {
				result.addAll(resolve(value, newTraversedContexts, newTraversedChunks, remainingChunks.subList(1, remainingChunks.size())));
			}
			return result;
		}

		if (context instanceof List) {
			List<Object> result = new ArrayList<>();

			List<Object> list = (List<Object>) context;
			if (currentChunk.equals("*")) {
				for (int i = 0; i < list.size(); i++) {
					Object value = list.get(i);

					List<Object> newTraversedContexts = new ArrayList<>(traversedContexts);
					newTraversedContexts.add(context);

					List<String> newTraversedChunks = new ArrayList<>(traversedChunks);
					newTraversedChunks.add(String.valueOf(i + 1));

					if (remainingChunks.size() == 1) {
						result.add(createPathMatch(stringifyPath(newTraversedChunks), value));
					}
					else {
						result.addAll(resolve(value, newTraversedContexts, newTraversedChunks, remainingChunks.subList(1, remainingChunks.size())));
					}
				}
			}
			else {
				Object value;
				try {
					value = list.get(Integer.parseInt(currentChunk) - 1);
				}
				catch (Exception e) {
					return Collections.emptyList();
				}

				List<Object> newTraversedContexts = new ArrayList<>(traversedContexts);
				newTraversedContexts.add(context);

				List<String> newTraversedChunks = new ArrayList<>(traversedChunks);
				newTraversedChunks.add(currentChunk);

				if (remainingChunks.size() == 1) {
					result.add(createPathMatch(stringifyPath(newTraversedChunks), value));
				}
				else {
					result.addAll(resolve(value, newTraversedContexts, newTraversedChunks, remainingChunks.subList(1, remainingChunks.size())));
				}
			}
			return result;
		}

		return Collections.emptyList();
	}

	// {decide: function(url, namespace, input) external {java : {class: "de.materna.jdec.dmn.functions.FunctionProvider", method signature: "decide(java.lang.String, java.lang.String, java.util.Map)"}}}.decide("http://decnet.materna.de", "0003-input-data-string-allowed-values", {"Employment Status": "UNEMPLOYED"})
	public static Object decide(String url, String namespace, Map<String, Object> input) {
		try {
			HttpPost httpRequest = new HttpPost(url + "/" + namespace);
			httpRequest.setHeader("Content-Type", "application/json");
			httpRequest.setEntity(new StringEntity(SerializationHelper.getInstance().toJSON(input)));

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
				String s = EntityUtils.toString(httpResponse.getEntity());
				return SerializationHelper.getInstance().toClass(s, new TypeReference<HashMap<String, Object>>() {
				}).get("outputs");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
