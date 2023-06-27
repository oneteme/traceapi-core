package org.usf.traceapi.core;

import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.Helper.localTrace;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class IncomingRequestInterceptor implements HandlerInterceptor { //AsyncHandlerInterceptor ?

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return handler instanceof HandlerMethod; //important! !static resource 
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) throws Exception {
        HandlerMethod m = (HandlerMethod) handler;
        TraceableApi a = m.getMethodAnnotation(TraceableApi.class);
        if(nonNull(a)) {
        	var trace = (IncomingRequest) localTrace.get();
            if(nonNull(trace) && a.clientProvider() != ClientProvider.class) {
        		trace.setClient(supplyClient(req, a.clientProvider()));
            }
        }
    }
    
    private static String supplyClient(HttpServletRequest req, Class<? extends ClientProvider> clasz) { //simple impl.
		try {
			return clasz.getDeclaredConstructor().newInstance().supply(req);
		} catch (Exception e) {
			log.warn("cannot instantiate class " + clasz, e);
		}
		return null;
    }
    
}
