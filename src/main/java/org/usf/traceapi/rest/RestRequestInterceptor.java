package org.usf.traceapi.rest;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Session.appendSessionStage;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.rest.RestSessionFilter.TRACE_HEADER;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.usf.traceapi.core.RestRequest;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class RestRequestInterceptor implements ClientHttpRequestInterceptor {
	
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return call(()-> execution.execute(request, body), (s,e,res,t)->{
			var req = new RestRequest();
			req.setMethod(request.getMethod().name());
			req.setProtocol(request.getURI().getScheme());
			req.setHost(request.getURI().getHost());
			req.setPort(request.getURI().getPort());
			req.setPath(request.getURI().getPath());
			req.setQuery(request.getURI().getQuery());
			req.setAuthScheme(extractAuthScheme(request.getHeaders().get(AUTHORIZATION)));
			req.setStart(s);
			req.setEnd(e);
			req.setOutDataSize(nonNull(body) ? body.length : -1);
			req.setOutContentEncoding(request.getHeaders().getFirst(CONTENT_ENCODING)); 
			req.setException(mainCauseException(t));
			req.setThreadName(threadName());
			stackTraceElement().ifPresent(st->{
				req.setName(st.getMethodName());
				req.setLocation(st.getClassName());
			});
//			setUser if auth=Basic !
			if(nonNull(res)) {
				req.setStatus(res.getStatusCode().value());
				req.setInDataSize(res.getBody().available()); //estimated !
				req.setContentType(ofNullable(res.getHeaders().getContentType()).map(MediaType::getType).orElse(null));
				req.setOutContentEncoding(res.getHeaders().getFirst(CONTENT_ENCODING)); 
				req.setId(res.getHeaders().getFirst(TRACE_HEADER)); //+ send api_name !?
			}
			appendSessionStage(req);
		});
	}
}