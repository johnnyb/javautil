package net.bplearning.util.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import net.bplearning.util.util.IOUtil;

/**
 * This class simplifies making HTTP requests.
 */
public class EasyHttp {
	private String urlBase;
	private Map<String, String> standardHeaders;
	private Map<?, ?> standardParams;

	/**
	 * This class is largely a wrapper around the
	 * HttpURLConnection class, but does some
	 * handling of the output stream for you.
	 */
	public static class Response {
		HttpURLConnection connection;
		byte[] data = new byte[0];
		boolean dataLoaded = false;

		Response(HttpURLConnection conn) {
			this.connection = conn;
		}

		public byte[] getResponseData() {
			loadData();
			return data;
		}

		public String getResponseAsString() {
			loadData();
			try {
				return new String(data, "UTF-8");
			} catch(UnsupportedEncodingException e) {
				return "";
			}
		}

		/**
		 * Loads the data with the request.  This is called
		 * internally so it is not normally needed.  However,
		 * if there is a significant delay between making the
		 * request and when you need the data, you can call
		 * this to keep the connection open.
		 */
		public void loadData() {
			// Only try this once
			if(dataLoaded) {
				return; // Fast, non-synchronized path
			}
			synchronized(this) {
				if(dataLoaded) {
					return;
				} else {
					dataLoaded = true;
				}
			}

			try {
				data = IOUtil.readStreamToEnd(connection.getInputStream());
			} catch (IOException e) {
				// Do nothing
			}
		}

		public HttpURLConnection getConnection() {
			return connection;
		}

		public int getResponseCode() {
			try {
				return connection.getResponseCode();
			} catch(IOException e) {
				return 0;
			}
		}
	}

	public EasyHttp(String baseUrl, Map<String, String> headers, Map<?, ?> params) {
		urlBase = baseUrl;
		standardHeaders = headers;
		standardParams = params;
	}

	/**
	 * Generates a connection for the given parameters.  The connection
	 * is not opened before returning.
	 * @param method the request method (GET/POST/PUT/etc)
	 * @param endpoint the endpoint requested
	 * @param params the URL parameters
	 * @param headers any additional headers to send
	 * @return an unopened {@link HttpURLConnection}
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public HttpURLConnection generateConnectionForRequest(String method, String endpoint, Map<?, ?> params, Map<String, String> headers) throws MalformedURLException, IOException {
		// Build URL
		String urlString = buildUrlString(urlBase, endpoint, standardParams, params).toString();
		URL dest = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) dest.openConnection();

		// Build Headers
		for(Map.Entry<String, String> hdr: headers.entrySet()) {
			conn.addRequestProperty(hdr.getKey(), hdr.getValue());
		}
		for(Map.Entry<String, String> hdr: standardHeaders.entrySet()) {
			conn.addRequestProperty(hdr.getKey(), hdr.getValue());
		}

		// Set request method
		conn.setRequestMethod(method);

		// Should I turn off caching?  Timeouts?

		return conn;
	}

	/**
	 * Performs an HTTP request.  Returns the HttpURLConnection object.
	 * @param method
	 * @param endpoint
	 * @param params
	 * @param body
	 * @param headers
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public Response performRequest(String method, String endpoint, Map<?, ?> params, byte[] body, Map<String, String> headers) throws MalformedURLException, IOException {
		HttpURLConnection conn = generateConnectionForRequest(method, endpoint, params, headers);

		boolean hasBody = body != null && body.length != 0;

		if(hasBody) {
			conn.setDoOutput(true);
		}

		conn.connect();

		if(hasBody) {
			OutputStream ostream = conn.getOutputStream();
			ostream.write(body);
			ostream.close();
		}

		return new Response(conn);
	}

	/**
	 * Builds a URL from standard components.
	 * @param base the base URL for the endpoint.  Should include the protocol.
	 * @param endpoint the remaining portion of the URL
	 * @param params any needed parameters.  Should be in the format needed by {@link EasyHttp#buildQueryString(Map)}.
	 * @return
	 */
	public static CharSequence buildUrlString(String base, String endpoint, Map<?, ?> params) {
		return buildUrlString(base, endpoint, params, null);
	}

	// This takes two different parameter sets so that there can be an initial,
	// standard one as well as a request-specific one.
	protected static CharSequence buildUrlString(String base, String endpoint, Map<?, ?> params, Map<?, ?> moreParams) {
		StringBuilder sb = new StringBuilder(base.length() + endpoint.length() + 500);

		// Base URL
		if(base != null) {
			sb.append(base);
		}
		sb.append(endpoint);

		CharSequence p1 = buildQueryString(params);
		CharSequence p2 = buildQueryString(moreParams);

		if(p1.length() != 0 || p2.length() != 0) {
			if(endpoint.contains("?")) {
				sb.append('&');
			} else {
				sb.append('?');
			}
		}

		sb.append(p1);
		if(p1.length() != 0 && p2.length() != 0) {
			sb.append('&');
		} 
		sb.append(p2);

		return sb;
	}

	/**
	 * Converts a {@link java.util.Map} into a query string.
	 * @param params the parameters to add.  {@link Object#toString()} is called on each key and value.
	 * @return
	 */
	public static CharSequence buildQueryString(Map<?, ?> params) {
		if(params == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(params.size() * 30);
		boolean first = true;
		for(Map.Entry<?, ?> entry: params.entrySet()) {
			try {
				Object unencodedKey = entry.getKey();
				Object unencodedValue = entry.getValue();
			
				String key = URLEncoder.encode(unencodedKey.toString(), "UTF-8");
				String value = URLEncoder.encode(unencodedValue.toString(), "UTF-8");

				if(first) {
					first = false;
				} else {
					sb.append("&");
				}

				sb.append(key);
				sb.append("=");
				sb.append(value);
			} catch(Exception e) {
				// Not likely
			}
		}

		return sb;
	}
}
