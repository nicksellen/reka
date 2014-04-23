package reka.smtp;

import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.background;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowSegment;
import reka.api.run.EverythingSubscriber;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.configurer.annotations.Conf;
import reka.core.bundle.RekaBundle;
import reka.core.bundle.SetupTrigger;
import reka.core.bundle.TriggerConfigurer;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;

public class SmtpBundle implements RekaBundle {
	
	private static final Logger log = LoggerFactory.getLogger(SmtpBundle.class);

	public void setup(Setup setup) {
		setup.use(path("smtp"), () -> new UseSMTPConfigurer());
	}
	
	public static class UseSMTPConfigurer extends UseConfigurer {
		
		private String host, username, password;
		private int port = 25;
		
		@Conf.At("host")
		public void host(String val) {
			host = val;
		}
		
		@Conf.At("port")
		public void port(String val) {
			port = Integer.valueOf(val);
		}
		
		@Conf.At("username")
		public void username(String val) {
			username = val;
		}
		
		@Conf.At("password")
		public void password(String val) {
			password = val;
		}

		@Override
		public void setup(UseInit init) {
			init.operation("send", () -> new SMTPSendConfigurer(host, port, username, password));
			init.trigger(root(), () -> new SMTPTriggerThing());
		}
		
	}
	
	public static class SMTPSendConfigurer implements Supplier<FlowSegment> {

		private String host, username, password;
		private int port;
		
		private StringWithVars from, to, replyTo, subject, body;
		
		public SMTPSendConfigurer(String host, int port, String username, String password) {
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
		public FlowSegment get() {
			return background("smtp/send", () -> new SMTPSendOperation(
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
	
	public static class SMTPSendOperation implements SyncOperation {

		private final String host, username, password;
		private final int port;
		
		private final StringWithVars from, to, replyTo, subject, body;
		
		public SMTPSendOperation(
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
		public MutableData call(final MutableData data) {
			
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
				
				return data;
				
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
	
	public static class SMTPTriggerThing implements TriggerConfigurer {

		private int port = 25;
		private Path flowName;
		
		@Conf.Config
		public void config(Config config) {
			checkConfig(config.hasValue(), "must have a value (for the port)");
			port = config.valueAsInt();
		}
		
		@Conf.At("run")
		public void run(String val) {
			flowName = path(val); // FIXME: namespacing?
		}
		
		@Override
		public void setupTriggers(SetupTrigger trigger) {
			
			trigger.onStart(app -> {
				
				SMTPServer smtpServer = new SMTPServer(
					new SimpleMessageListenerAdapter(
						new EmailListener(app.flows().flow(flowName))));
				
				smtpServer.setPort(port);
				
				log.debug("starting smtp server on port {}", port);
				
				app.register(port, "smtp", MutableMemoryData.create(details -> {
					details.putString("run", flowName.last().toString());
				}).readonly());
				
				smtpServer.start();
				
				app.onUndeploy((v) -> {
					smtpServer.stop();
				});
				
			});
			
		}
		
	}
	
	public static class EmailListener implements SimpleMessageListener {
		
		private final Flow flow;
		
		public EmailListener(Flow flow) {
			this.flow = flow;
		}

		@Override
		public boolean accept(String from, String to) {
			return true;
		}

		@Override
		public void deliver(String from, String to, InputStream stream) throws TooMuchDataException, IOException {
			
			Properties props = new Properties();
			Session session = Session.getInstance(props);
			try {
				
				MimeMessage message = new MimeMessage(session, stream);
				
				MimeMessageParser email = new MimeMessageParser(message);
				
				email.parse();
				
				stream.close();
				
				MutableData data = MutableMemoryData.create();
				
				MutableData emailData = data.createMapAt(path("email"));
				
				for (Address addr : email.getTo()) {
					emailData.putOrAppend(path("to"), utf8(addr.toString()));
				}
				
				for (Address addr : email.getCc()) {
					emailData.putOrAppend(path("cc"), utf8(addr.toString()));
				}
				
				for (Address addr : email.getBcc()) {
					emailData.putOrAppend(path("bcc"), utf8(addr.toString()));
				}
				
				if (email.getReplyTo() != null) {
					emailData.putString("reply-to", email.getReplyTo());
				}
				
				if (email.getFrom() != null) {
					emailData.putString("from", email.getFrom());
				}
				
				for (DataSource att : email.getAttachmentList()) {
					
					emailData.putMap(path(name("attachments"), nextIndex()), map -> {
						map.putString("name", att.getName());
						map.putString("content-type", att.getContentType());
						try {
							map.put("content", binary(att.getContentType(), att.getInputStream()));
						} catch (Exception e) {
							throw unchecked(e);
						}
					});
					
				}
				
				if (email.getPlainContent() != null) {
					emailData.putString(path("content", "plain"), email.getPlainContent());
				}
				
				if (email.getHtmlContent() != null) {
					emailData.putString(path("content", "html"), email.getHtmlContent());
				}
				
				emailData.putString("subject", email.getSubject());
				
				flow.prepare()
					.data(data)
					.complete(new EverythingSubscriber(){

						@Override
						public void ok(MutableData data) {
							log.debug("ok!");
						}

						@Override
						public void halted() {
							log.debug("halted :(");
						}

						@Override
						public void error(Data data, Throwable t) {
							log.debug("error :(");
							t.printStackTrace();
						}
					}).run();
				
			} catch (Exception e) {
				throw unchecked(e);
			}
		}
		
	}
	
}
