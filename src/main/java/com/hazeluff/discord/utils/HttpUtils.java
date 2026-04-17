package com.hazeluff.discord.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazeluff.discord.Config;

public class HttpUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

	public static String get(URI uri) throws HttpException {
		int retries = Config.HTTP_REQUEST_RETRIES;
		return get(uri, retries);
	}

	public static String get(URI uri, int maxRetries) throws HttpException {
		int retries = maxRetries;
		int httpStatusCode = -1;

		CloseableHttpClient client = HttpClientBuilder.create()
			.setDefaultSocketConfig(
				SocketConfig.custom()
					.setSoTimeout(10000)
					.build()
			)
			.setConnectionTimeToLive(10, TimeUnit.SECONDS).build();

		HttpGet request = new HttpGet(uri);
		RequestConfig requestConfig = RequestConfig.custom()
			.setMaxRedirects(1)
			.setSocketTimeout(10000)
			.setConnectTimeout(10000)
			.setConnectionRequestTimeout(10000)
			.build();
		request.setConfig(requestConfig);

		BufferedReader br = null;
		InputStreamReader isr = null;
		InputStream is = null;

		CloseableHttpResponse response = null;

		try {
			do {
				try {
					response = client.execute(request);
					httpStatusCode = response == null ? -1 : response.getStatusLine().getStatusCode();
				} catch (Throwable e) {
					LOGGER.error("Failed to request page [" + uri.toString() + "]", e);
					Utils.sleep(10000);
				}
				if (response == null || httpStatusCode != 200 && retries > 0)
					Utils.sleep(5000 * retries);
			} while ((response == null || httpStatusCode != 200) && retries-- > 0);

			if ((response == null || httpStatusCode != 200) && retries <= 0) {
				String message = "Failed to get page after (" + maxRetries + ") retries.";
				TimeoutException exception = new TimeoutException(message);
				throw exception;
			}
			is = response.getEntity().getContent();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			StringBuffer result = new StringBuffer();
			String line = "";
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			response.getEntity().getContent().close();
			return result.toString();
		} catch (Throwable e) {
			LOGGER.error("Error occurred.", e);
			throw new HttpException(e);
		} finally {
			try {
				request.reset();
			} catch (Throwable e) {
				LOGGER.error("Error closing request", e);
			}

			try {
				client.close();
			} catch (Throwable e) {
				LOGGER.error("Error closing client", e);
			}

			if (br != null) {
				try {
					br.close();
				} catch (Throwable e) {
					LOGGER.error("Error closing BufferedReader", e);
				}
			}

			if (isr != null) {
				try {
					isr.close();
				} catch (Throwable e) {
					LOGGER.error("Error closing InputStreamReader", e);
				}
			}

			if (is != null) {
				try {
					is.close();
				} catch (Throwable e) {
					LOGGER.error("Error closing InputStream", e);
				}
			}
		}
	}

	public static String getAndRetry(URI uri, int retries, long sleepMs, String description) throws HttpException {
		try {
			return Utils.getAndRetry(() -> get(uri), retries, sleepMs, description);
		} catch (TimeoutException e) {
			throw new HttpException(e);
		}
	}

	public static URI buildUri(String url) throws HttpException {
		try {
			URIBuilder uriBuilder = new URIBuilder(url);
			return uriBuilder.build();
		} catch (URISyntaxException e) {
			String message = "Error building URI";
			HttpException runtimeException = new HttpException(message, e);
			LOGGER.error(message, runtimeException);
			throw runtimeException;
		}
	}
}
