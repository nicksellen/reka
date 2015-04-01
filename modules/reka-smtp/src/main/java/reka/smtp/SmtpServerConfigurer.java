package reka.smtp;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import reka.api.IdentityKey;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.Subscriber;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleSetupContext;

public class SmtpServerConfigurer extends ModuleConfigurer {

	private static final Logger log = LoggerFactory.getLogger(SmtpServerConfigurer.class);
	
	public static final IdentityKey<RekaSmtpServer> SERVER = IdentityKey.named("SMTP server");

	private final Map<Integer,RekaSmtpServer> servers;
	private ConfigBody emailHandler;
	private int port = 25;
	
	public SmtpServerConfigurer(Map<Integer,RekaSmtpServer> servers) {
		this.servers = servers;
	}

	@Conf.At("port")
	public void port(String val) {
		port = Integer.valueOf(val);
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
	
	public class RekaSmtpServer implements Consumer<Data> {
		
		private final SMTPServer server;
		private final int port;
		
		private final Set<Flow> flows = new HashSet<>();
		
		public RekaSmtpServer(int port) {
			this.port = port;
			server = new SMTPServer(new SimpleMessageListenerAdapter(new EmailListener(this)));
			server.setPort(port);
		}
		
		private void start() {
			if (!server.isRunning()) {
				log.info("starting smtp server on port {}", port);
				server.start();
			}
		}
		
		public void stop() {
			if (server.isRunning() && flows.isEmpty()) {
				log.info("stopping smtp server on port {}", port);
				server.stop();
				servers.remove(port);
			}
		}
		
		public void add(Flow flow) {
			flows.add(flow);
			start();
		}
		
		public void remove(Flow flow) {
			flows.remove(flow);
			stop();
		}
		
		@Override
		public void accept(Data data) {
			flows.forEach(flow -> {
				flow.prepare()
				.mutableData(MutableMemoryData.create().merge(data))
				.complete(new Subscriber(){

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
			});
		}
		
	}
	
	@Override
	public void setup(AppSetup app) {
		
		ModuleSetupContext ctx = app.ctx();
		
		if (emailHandler != null) {
			
			app.onDeploy(init -> {
				init.run("start smtp server", () -> {
					RekaSmtpServer server = servers.computeIfAbsent(port, p -> new RekaSmtpServer(port));
					server.start();
					ctx.put(SERVER, server);
				});
			});
			
			app.buildFlow("on email", emailHandler, flow -> {
				RekaSmtpServer server = app.ctx().get(SERVER);
				server.add(flow);
				app.registerNetwork(port, "smtp");
				app.onUndeploy("undeploy smtp", () -> server.remove(flow));
			});
			
			app.onUndeploy("stop smtp server", () -> ctx.get(SERVER).stop());
			
		}
	}
	
}