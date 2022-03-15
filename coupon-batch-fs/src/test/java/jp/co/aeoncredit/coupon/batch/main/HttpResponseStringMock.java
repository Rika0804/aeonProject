/**
 * 
 */
package jp.co.aeoncredit.coupon.batch.main;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.net.ssl.SSLSession;

/**
 * @author u2004033
 *
 */
public class HttpResponseStringMock implements HttpResponse<String> {
	
	public int statusCode;
	public String body;
	
	public HttpResponseStringMock() {
		super();
	}
	
	public HttpResponseStringMock(int statusCode, String body) {
		this.statusCode = statusCode;
		this.body = body;
	}
	
	@Override
	public int statusCode() {
		return statusCode;
	}

	@Override
	public HttpRequest request() {
		return null;
	}

	@Override
	public Optional<HttpResponse<String>> previousResponse() {
		return null;
	}

	@Override
	public HttpHeaders headers() {
		return null;
	}

	@Override
	public String body() {
		return null;
	}

	@Override
	public Optional<SSLSession> sslSession() {
		return null;
	}

	@Override
	public URI uri() {
		return null;
	}

	@Override
	public Version version() {
		return null;
	}
}
