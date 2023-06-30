package org.usf.traceapi.core;

import static java.lang.String.format;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
public class OutcomingRequest implements Metric {

	private final String id;
	private String method;
	private String protocol;
	private String host;
	private int port;
	private String path;
	private String query; //nullable
	private String contentType; //nullable
	private String authScheme; //nullable   Basic, Bearer, Digest, OAuth, ..
	private Integer status; //nullable
	private long inDataSize;
	private long outDataSize;
 	private Instant start;
	private Instant end;
	private String threadName;
	
	@JsonCreator
	public OutcomingRequest(String id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return format("%-20s", threadName) + ": REQUEST {" +  format("%5s", duration()) + "ms}";
	}
}