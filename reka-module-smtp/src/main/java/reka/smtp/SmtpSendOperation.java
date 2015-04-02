package reka.smtp;

import static reka.util.Util.unchecked;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.core.util.StringWithVars;

public class SmtpSendOperation implements Operation {

	private final String host, username, password;
	private final int port;
	
	private final StringWithVars from, to, replyTo, subject, body;
	
	public SmtpSendOperation(
			String host, 
			int port, 
			String username,
			String password,
			StringWithVars from, 
			StringWithVars to, 
			StringWithVars replyTo, 
			StringWithVars subject, 
			StringWithVars body) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.from = from;
		this.to = to;
		this.replyTo = replyTo;
		this.subject = subject;
		this.body = body;
	}
	
	@Override
	public void call(final MutableData data, OperationContext ctx) {
		
		try {
			
			SimpleEmail email = new SimpleEmail();
			
			email.setHostName(host);
			email.setSmtpPort(port);
			
			if (username != null && password != null) {
				email.setAuthentication(username, password);
			}
			
			email.setSubject(subject.apply(data));
			email.setFrom(from.apply(data));
			email.addTo(to.apply(data));
			
			if (replyTo != null) {
				email.addReplyTo(replyTo.apply(data));
			}
			
			email.setMsg(body.apply(data));
			
			email.send();
			
			/*
			return executor.submit(new Callable<MutableData>(){

				@Override
				public MutableData call() throws Exception {
					email.send();
					return data;
				}
				
			});
			*/
			
		} catch (EmailException e) {
			throw unchecked(e);
		}
		
	}
	
}