package org.usf.traceapi.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "api.tracing")
public final class TraceConfigurationProperties extends SessionDispatcherProperties {
	
	private String url = "";

}
