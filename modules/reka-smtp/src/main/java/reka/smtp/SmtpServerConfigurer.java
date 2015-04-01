package reka.smtp;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import reka.api.IdentityKey;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.Subscriber;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetupContext;

public class SmtpServerConfigurer extends ModuleConfigurer {

	private static final Logger log = LoggerFactory.getLogger(SmtpServerConfigurer.class);
	
	public static final IdentityKey<RekaSmtpServer> SERVER = IdentityKey.named("SMTP server");

	private final Map<Integer,RekaSmtpServer> servers;
	private final List<BiFunction<String,String,Boolean>> acceptors = new ArrayList<>();
	private ConfigBody emailHandler;
	private int port = 25;
	
	public SmtpServerConfigurer(Map<Integer,RekaSmtpServer> servers) {
		this.servers = servers;
	}

	@Conf.At("port")
	public void port(String val) {
		port = Integer.valueOf(val);
	}
	
	private static final Pattern REGEX_PATTERN = Pattern.compile("^/(.*)/$");
	
	@Conf.Each("from")
	public void from(String value) {
		Matcher m = REGEX_PATTERN.matcher(value);
		if (m.matches()) {
			acceptors.add(new FromPatternAcceptor(Pattern.compile(m.group(1))));
		} else {
			acceptors.add(new FromStringAcceptor(value));
		}
	}

	
	@Conf.Each("to")
	public void to(String value) {
		Matcher m = REGEX_PATTERN.matcher(value);
		if (m.matches()) {
			acceptors.add(new ToPatternAcceptor(Pattern.compile(m.group(1))));
		} else {
			acceptors.add(new ToStringAcceptor(value));
		}
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
	
	public static class FromPatternAcceptor implements BiFunction<String,String,Boolean> {

		private final Pattern pattern;
		
		public FromPatternAcceptor(Pattern pattern) {
			this.pattern = pattern;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return pattern.matcher(from).find();
		}
		
	}
	
	public static class ToPatternAcceptor implements BiFunction<String,String,Boolean> {

		private final Pattern pattern;
		
		public ToPatternAcceptor(Pattern pattern) {
			this.pattern = pattern;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return pattern.matcher(to).find();
		}
		
	}
	

	
	public static class FromStringAcceptor implements BiFunction<String,String,Boolean> {

		private final String match;
		
		public FromStringAcceptor(String match) {
			this.match = match;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return match.equals(from);
		}
		
	}
	
	public static class ToStringAcceptor implements BiFunction<String,String,Boolean> {

		private final String match;
		
		public ToStringAcceptor(String match) {
			this.match = match;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			return match.equals(to);
		}
		
	}
	
	private static final Path EMAIL_PATH = path("email");
	
	public class RekaSmtpServer implements Consumer<Data> {
		
		private final SMTPServer server;
		private final int port;
		private final EmailListener listener;
		
		private final Set<Flow> flows = new HashSet<>();
		
		public RekaSmtpServer(int port) {
			this.port = port;
			this.listener = new EmailListener(this);
			server = new SMTPServer(new SimpleMessageListenerAdapter(listener));
			server.setPort(port);
		}
		
		public void setAcceptor(BiFunction<String,String,Boolean> acceptor) {
			listener.setAcceptor(acceptor);
		}
		
		private void start() {
			if (!server.isRunning()) {
				log.info("starting smtp server on port {}", port);
				server.start();
			}
		}
		
		public void stopIfEmpty() {
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
			stopIfEmpty();
		}
		
		@Override
		public void accept(Data data) {
			flows.forEach(flow -> {
				flow.prepare()
				.mutableData(MutableMemoryData.create().put(EMAIL_PATH, data))
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
	
	public static class CombinedAcceptor implements BiFunction<String,String,Boolean> {

		private final Iterable<BiFunction<String,String,Boolean>> acceptors;
		
		public CombinedAcceptor(Iterable<BiFunction<String,String,Boolean>> acceptors) {
			this.acceptors = acceptors;
		}
		
		@Override
		public Boolean apply(String from, String to) {
			for (BiFunction<String, String, Boolean> acceptor : acceptors) {
				if (acceptor.apply(from, to)) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	private static final BiFunction<String,String,Boolean> ACCEPT_ALL = (from, to) -> true;
	
	@Override
	public void setup(AppSetup app) {
		
		ModuleSetupContext ctx = app.ctx();
		
		if (emailHandler != null) {
			
			app.requireNetwork(port);
			
			app.onDeploy(init -> {
				init.run("start smtp server", () -> {
					RekaSmtpServer server = servers.computeIfAbsent(port, p -> new RekaSmtpServer(port));
					if (acceptors.isEmpty()) {
						server.setAcceptor(ACCEPT_ALL);
					} else {
						server.setAcceptor(new CombinedAcceptor(acceptors));
					}
					server.start();
					ctx.put(SERVER, server);
				});
			});
			
			app.buildFlow("on email", emailHandler, flow -> {
				RekaSmtpServer server = ctx.require(SERVER);
				server.add(flow);
				app.registerNetwork(port, "smtp");
				app.onUndeploy("undeploy smtp", () -> server.remove(flow));
			});
			
			app.onUndeploy("stop smtp server", () -> ctx.get(SERVER).stopIfEmpty());
			
		}
	}
	
}