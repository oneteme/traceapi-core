package org.usf.traceapi.core;

import static java.util.Collections.synchronizedCollection;

import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author u$f
 *
 */
@Setter
@Getter
public final class IncomingRequest extends OutcomingRequest implements Session {

	private String name; // @annotation, endpoint
	private String user; //nullable
	private ApplicationInfo application;
	private final Collection<OutcomingRequest> requests;
	private final Collection<OutcomingQuery> queries;
	
	public IncomingRequest(String id) {
		this(id, new LinkedList<>(), new LinkedList<>());
	}
	
	@JsonCreator
	public IncomingRequest(String id, Collection<OutcomingRequest> requests, Collection<OutcomingQuery> queries) {
		super(id);
		this.requests = requests;
		this.queries = queries; 
	}
	
	public void append(OutcomingRequest request) {
		requests.add(request);
	}

	public void append(OutcomingQuery query) {
		queries.add(query);
	}
	
	static IncomingRequest synchronizedIncomingRequest(String id) {
		return new IncomingRequest(id, 
				synchronizedCollection(new LinkedList<>()), 
				synchronizedCollection(new LinkedList<>()));
	}
	
}