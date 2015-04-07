package reka.email.imap;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder.FetchProfileItem;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.email.util.EmailToDataConverter;
import reka.identity.IdentityKey;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.module.setup.ModuleSetupContext;

import com.sun.mail.imap.IMAPFolder;

public class ImapConfigurer extends ModuleConfigurer {

	private static final Logger log = LoggerFactory.getLogger(ImapConfigurer.class);

	public static final IdentityKey<ImapIdleConnection> CONNECTION = IdentityKey.named("imap connection");

	private String hostname, username, password;
	private Integer port;
	private boolean tls = true;
	private ConfigBody emailHandler;
	private String folderName = "INBOX";

	@Conf.At("hostname")
	public void hostname(String val) {
		hostname = val;
	}

	@Conf.At("username")
	public void username(String val) {
		username = val;
	}

	@Conf.At("password")
	public void password(String val) {
		password = val;
	}

	@Conf.At("port")
	public void port(String val) {
		port = Integer.valueOf(val);
	}

	@Conf.At("tls")
	public void tls(boolean val) {
		tls = val;
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		checkConfig(config.hasBody(), "must have a body");
		switch (config.valueAsString()) {
		case "email":
			emailHandler = config.body();
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(AppSetup module) {

		ModuleSetupContext ctx = module.ctx();
		
		if (emailHandler != null) {
			module.buildFlow("on email", emailHandler, flow -> {
				/*
				ctx.lookup(CONNECTION).ifPresent(connection -> {
					connection.stop();
				});
				*/
				ImapIdleConnection connection = new ImapIdleConnection(hostname, port, tls, username, password, folderName, data -> {
					flow.prepare().data(data).run();
				});
				connection.start();
				ctx.put(CONNECTION, connection);
			});
		}

		module.onUndeploy("close imap connection", () -> {
			ctx.lookup(CONNECTION).ifPresent(connection -> {
				connection.stop();
			});
		});

	}

	public static class RekaMessageCountListener implements MessageCountListener {

		private final IMAPFolder folder;
		private final Consumer<Data> consumer;
		
		public RekaMessageCountListener(IMAPFolder folder, Consumer<Data> consumer) {
			this.folder = folder;
			this.consumer = consumer;
		}
		
		@Override
		public void messagesAdded(MessageCountEvent e) {
			
			FetchProfile fp = new FetchProfile();
		    fp.add(FetchProfile.Item.ENVELOPE);
		    fp.add(FetchProfileItem.FLAGS);
		    fp.add(FetchProfileItem.CONTENT_INFO);
		    
		    try {
		    	
		    	// this is supposed to prevent multiple roundtrips for each message
		    	// but still has one for each message. I think.
		    	// see http://stackoverflow.com/questions/8322836/javamail-imap-over-ssl-quite-slow-bulk-fetching-multiple-messages
		    	
				folder.fetch(e.getMessages(), fp);

				for (Message msg : e.getMessages()) {
					consumer.accept(EmailToDataConverter.convert(msg));
				}
				
			} catch (MessagingException t) {
				log.warn("error processing email", t);
			}
		    
		}

		@Override
		public void messagesRemoved(MessageCountEvent e) {
			
		}
		
	}

	public static class ImapIdleConnection {

		private final String hostname, username, password;
		private final boolean tls;
		private final int port;
		private final String folderName;
		private final Consumer<Data> consumer;

		private final Object lock = new Object();
		
		private volatile boolean active = false;
		
		// when active is true all these things MUST be set
		private volatile Store store;
		private volatile IMAPFolder folder;
		private volatile ScheduledFuture<?> keepAlive;
		private volatile Thread imapIdleThread;
		private volatile RekaMessageCountListener listener;

		public ImapIdleConnection(String hostname, int port, boolean tls, String username, String password, String folderName, Consumer<Data> consumer) {
			this.hostname = hostname;
			this.port = port;
			this.tls = tls;
			this.username = username;
			this.password = password;
			this.folderName = folderName;
			this.consumer = consumer;
		}

		public void start() {
			synchronized (lock) {
				if (active) return;
				
				Properties props = new Properties();
				props.put("mail.imap.connectiontimeout", "3000");
				props.put("mail.imap.timeout", "3000");
				Session session = Session.getDefaultInstance(props);
				
				try {
					
					store = session.getStore(tls ? "imaps" : "imap");
					
					log.info("connecting to {}:{} as {} tls:{}", hostname, port, username, tls);
					store.connect(hostname, port, username, password);
					log.info("connected");
					folder = (IMAPFolder) store.getFolder(folderName);
					folder.open(Folder.READ_ONLY);
					
					listener = new RekaMessageCountListener(folder, consumer);
					
					folder.addMessageCountListener(listener);
					
					imapIdleThread = new ImapIdleThread(folder);
					imapIdleThread.start();
					
					keepAlive = startKeepAlive();
					
					active = true;
					
				} catch (Exception e) {
					log.warn("oops", e);
				}
				
			}
		}

		public void stop() {
			synchronized (lock) {
				if (!active) return;
				
				log.info("disconnecting from {}:{}", hostname, port);
				
				imapIdleThread.interrupt();
				
				keepAlive.cancel(false);
				
				folder.removeMessageCountListener(listener);
				
				try {
					folder.close(false);
				} catch (MessagingException e) { /* whatever */ }

				try {
					store.close();
				} catch (MessagingException e) { /* whatever */ }
				
				listener = null;
				folder = null;
				store = null;
				keepAlive = null;
				imapIdleThread = null;

				active = false;
			}
		}

		private ScheduledFuture<?> startKeepAlive() {
			return Reka.SharedExecutors.scheduled.scheduleAtFixedRate(() -> {
				Reka.SharedExecutors.general.execute(() ->  {
					try {
						synchronized (lock) {
							if (active) {
								log.debug("executing imap NOOP");
								folder.doCommand(cmd -> {
									cmd.simpleCommand("NOOP", null);
									return null;
								});
							}
						}
					} catch (Exception e) {
						log.warn("error running NOOP", e);
					}
				});
			}, 0, 5, TimeUnit.MINUTES);
		}
	}
	
	public static class ImapIdleThread extends Thread {
	
		private final IMAPFolder folder;
		
		public ImapIdleThread(IMAPFolder folder) {
			super("reka-email-imap-idle");
			this.folder = folder;
		}
		
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				log.debug("issuing IMAP IDLE");
				try {
					if (!folder.isOpen()) {
						break;
					}
					folder.idle();
				} catch (MessagingException e) {
					log.error("exception issuing IDLE", e);
					break;
				}
			}
		}
		
	}

}
