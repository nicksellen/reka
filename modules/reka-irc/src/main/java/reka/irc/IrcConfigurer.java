package reka.irc;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.builder.TriggerHelper;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class IrcConfigurer extends ModuleConfigurer {
	
	private static final IdentityKey<Flow> MESSAGE = IdentityKey.named("on message");
	private static final IdentityKey<Flow> PRIVATE_MESSAGE = IdentityKey.named("on private message");
	
	private final TriggerHelper triggers = new TriggerHelper();
	
	public static final IdentityKey<RekaBot> BOT = IdentityKey.named("bot");
	
	private String name;
	private String hostname;
	private String channel;
	private String key;

	@Conf.At("name")
	public void name(String val) {
		name = val;
	}
	
	@Conf.At("hostname")
	public void hostname(String val) {
		hostname = val;
	}

	@Conf.At("channel")
	public void channel(String val) {
		channel = val;
	}
	
	@Conf.At("key")
	public void key(String val) {
		key = val;
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		switch (config.valueAsString()) {
		case "message":
			triggers.add(MESSAGE, config.body());
			break;
		case "private message":
			triggers.add(PRIVATE_MESSAGE, config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(ModuleSetup app) {

		app.defineOperation(path("send"), provider -> new IrcSendConfigurer());
		
		app.onDeploy(init -> {
			init.run("create bot", ctx -> {
				ctx.put(BOT, new RekaBot(name, hostname, channel, key));
			});
		});

		app.onUndeploy("disconnect", ctx -> {
			ctx.remove(BOT).ifPresent(RekaBot::shutdown);
		});

		app.buildFlows(triggers.build(), reg -> {
			RekaBot bot = app.ctx().get(BOT);
			reg.flowFor(MESSAGE).ifPresent(flow -> {
				bot.addListener(new IrcMessageFlowListener(flow));
			});	
			reg.flowFor(PRIVATE_MESSAGE).ifPresent(flow -> {
				bot.addListener(new IrcPrivateMessageFlowListener(flow));
			});	
			bot.connect();
		});
		
	}
}
