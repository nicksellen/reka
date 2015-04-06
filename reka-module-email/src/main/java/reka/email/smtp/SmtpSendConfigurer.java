package reka.email.smtp;

import reka.config.configurer.annotations.Conf;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class SmtpSendConfigurer implements OperationConfigurer {

	private String host, username, password;
	private int port;
	
	private StringWithVars from, to, replyTo, subject, body;
	
	public SmtpSendConfigurer(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	@Conf.At("to")
	public void to(String val) {
		to = StringWithVars.compile(val);
	}

	@Conf.At("reply-to")
	public void replyTo(String val) {
		replyTo = StringWithVars.compile(val);
	}
	
	@Conf.At("from")
	public void from(String val) {
		from = StringWithVars.compile(val);
	}
	
	@Conf.At("subject")
	public void subject(String val) {
		subject = StringWithVars.compile(val);
	}
	
	@Conf.At("body")
	public void body(String val) {
		body = StringWithVars.compile(val);
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("send", () -> new SmtpSendOperation(
				host, 
				port, 
				username, 
				password, 
				from, 
				to, 
				replyTo, 
				subject, 
				body));
	}
	
}