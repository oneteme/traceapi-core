package org.usf.traceapi.mail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.stackTraceElement;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.MailRequest.newMailRequest;
import static org.usf.traceapi.core.Session.appendSessionStage;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.mail.MailAction.CONNECTION;
import static org.usf.traceapi.mail.MailAction.DISCONNECTION;
import static org.usf.traceapi.mail.MailAction.SEND;

import java.util.stream.Stream;

import org.usf.traceapi.core.Mail;
import org.usf.traceapi.core.MailRequest;
import org.usf.traceapi.core.MailRequestStage;
import org.usf.traceapi.core.StageTracker.StageConsumer;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransportWrapper { //cannot extends jakarta.mail.Transport
	
	@Delegate
	private final Transport trsp;
	private MailRequest req = newMailRequest(); //avoid nullPointer
	
	public void connect() throws MessagingException {
		exec(trsp::connect, appendConnection(null, null, null));
	}

	public void connect(String user, String password) throws MessagingException {
		exec(()->trsp.connect(user, password), appendConnection(null, null, user));
	}

	public void connect(String host, String user, String password) throws MessagingException {
		exec(()-> trsp.connect(host, user, password), appendConnection(host, null, user));
	}
	
	public void connect(String arg0, int arg1, String arg2, String arg3) throws MessagingException {
		exec(()-> trsp.connect(arg0, arg1, arg2, arg3), appendConnection(arg0, arg1, arg2));
	}

	public void sendMessage(Message arg0, Address[] arg1) throws MessagingException {
		try {
			exec(()-> trsp.sendMessage(arg0, arg1), appendAction(SEND));
		}
		finally { //safe
			var mail = new Mail();
			mail.setSubject(arg0.getSubject());
			mail.setFrom(toStringArray(arg0.getFrom()));
			mail.setContentType(arg0.getContentType());
			mail.setRecipients(toStringArray(arg0.getAllRecipients()));
			mail.setReplyTo(toStringArray(arg0.getReplyTo()));
			mail.setSize(arg0.getSize());
			req.getMails().add(mail);
		}
	}

	public void close() throws MessagingException {
		exec(trsp::close, (s,e,v,t)-> {
			appendAction(DISCONNECTION).accept(s, e, v, t);
			req.setEnd(e);
		});
	}
	
	StageConsumer<Void> appendConnection(String host, Integer port, String user) {
		return (s,e,v,t)-> {
			var url = ofNullable(trsp.getURLName());
			req = newMailRequest();
			req.setHost(url.map(URLName::getHost).orElse(host));
			req.setPort(url.map(URLName::getPort).orElse(port));
			req.setUser(url.map(URLName::getUsername).orElse(user));
			req.setStart(s);
			if(nonNull(t)) { //fail
				req.setEnd(e);
				//do not setException, already set in action
			}
			stackTraceElement().ifPresent(st->{
				req.setName(st.getMethodName());
				req.setLocation(st.getClassName());
			});
			req.setThreadName(threadName());
			appendAction(CONNECTION).accept(s, e, v, t);
			appendSessionStage(req);
		};
	}
	
	<T> StageConsumer<T> appendAction(MailAction action) {
		return (s,e,o,t)-> {
			var rs = new MailRequestStage();
			rs.setName(action.name());
			rs.setStart(s);
			rs.setEnd(e);
			rs.setException(mainCauseException(t));
			req.getActions().add(rs);
		};
	}
	
	private static String[] toStringArray(Address... address) {
		return isNull(address) 
			? null 
			: Stream.of(address).map(Address::toString).toArray(String[]::new);
	}
	
	public static TransportWrapper wrap(Transport trsp) {
		return new TransportWrapper(trsp);
	}
}
