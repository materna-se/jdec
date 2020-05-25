package de.materna.jdec.dmn.functions;

import de.materna.jdec.serialization.SerializationHelper;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class FunctionProvider {
	private static CloseableHttpClient httpClient = HttpClientBuilder.create().build();

	public static Object fetch(Map<String, Object> options) {
		// sort(list: {fetch: function(url) external {java : {class: "de.materna.jdec.drools.functions.FunctionProvider", method signature: "fetch(java.lang.String)"}}}.fetch("https://jsonplaceholder.typicode.com/users"), precedes: function(x,y) x.id < y.id)
		// {fetch: function(url, options) external {java : {class: "de.materna.jdec.drools.functions.FunctionProvider", method signature: "fetch(java.lang.String, java.util.Map)"}}}.fetch("http://httpbin.org/anything", {method: "POST", headers: {test: "jax"}, body: {"nein": true}})
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
}
