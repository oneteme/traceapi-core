package org.usf.traceapi.core;

import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.net.URI.create;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
import static org.usf.traceapi.core.ExceptionInfo.fromException;
import static org.usf.traceapi.core.Helper.applicationInfo;
import static org.usf.traceapi.core.Helper.defaultUserProvider;
import static org.usf.traceapi.core.Helper.extractAuthScheme;
import static org.usf.traceapi.core.Helper.idProvider;
import static org.usf.traceapi.core.Helper.localTrace;
import static org.usf.traceapi.core.Helper.log;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.IncomingRequest.synchronizedIncomingRequest;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class IncomingRequestFilter extends OncePerRequestFilter {

	static final Collector<CharSequence, ?, String> joiner = joining("_");
	static final String TRACE_HEADER = "x-tracert";
	
	private final TraceSender traceSender;
	private final String[] excludeUrlPatterns;
	
	private final AntPathMatcher matcher = new AntPathMatcher();
	
	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {
    	var in = synchronizedIncomingRequest(ofNullable(req.getHeader(TRACE_HEADER)).orElseGet(idProvider));
    	log.debug("incoming request : {} <= {}", in.getId(), req.getRequestURI());
    	localTrace.set(in);
    	var beg = currentTimeMillis();
    	try {
    		filterChain.doFilter(req, res);
    	}
    	catch(Exception e) {
    		if(isNull(in.getException())) {//already set in IncomingRequestInterceptor
    			in.setException(fromException(e));
    		}
    		throw e;
    	}
    	finally {
    		var fin = currentTimeMillis();
    		try {
	    		localTrace.remove();
	    		var uri = create(req.getRequestURL().toString());
	    		in.setMethod(req.getMethod());
	    		in.setProtocol(uri.getScheme());
	    		in.setHost(uri.getHost());
	    		if(uri.getPort() > 0) {
	    			in.setPort(uri.getPort());
	    		}
	    		in.setPath(req.getRequestURI()); // path
	    		in.setQuery(req.getQueryString());
	    		in.setContentType(res.getContentType());
				in.setStatus(res.getStatus());
				in.setAuthScheme(extractAuthScheme(req.getHeader(AUTHORIZATION)));
				in.setInDataSize(req.getInputStream().available()); //not exact !?
				in.setOutDataSize(res.getBufferSize()); //not exact !?
	    		in.setStart(ofEpochMilli(beg));
	    		in.setEnd(ofEpochMilli(fin));
	    		if(isNull(in.getName())) {//already set in IncomingRequestInterceptor
	    			in.setName(defaultEndpointName(req));
	    		}
	    		if(isNull(in.getUser())) {//already set in IncomingRequestInterceptor
	    			in.setUser(defaultUserProvider().getUser(req));
	    		}
    			in.setThreadName(threadName());
    			in.setApplication(applicationInfo());
    			traceSender.send(in);
    		}
    		catch(Exception e) {
				//do not catch exception
				log.warn("error while tracing : {}", req, e);
    		}
		}
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		return nonNull(excludeUrlPatterns) && Stream.of(excludeUrlPatterns)
		        .anyMatch(p -> matcher.match(p, request.getServletPath()));
	}
	
	@SuppressWarnings("unchecked")
	private static String defaultEndpointName(HttpServletRequest req) {
		var arr = req.getRequestURI().substring(1).split("/");
		var map = (Map<String, String>) req.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		return map == null ? join("_", arr) : Stream.of(arr)
				.filter(not(map.values()::contains))
				.collect(joiner);
	}
	
}