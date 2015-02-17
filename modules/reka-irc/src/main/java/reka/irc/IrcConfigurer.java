package reka.irc;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.irc.RekaBot.IrcListener;


public class IrcConfigurer extends ModuleConfigurer {
	
	public static final IdentityKey<RekaBot> BOT = IdentityKey.named("bot");
	
	private String name;
	private String hostname;
	private String channel;
	private String key;


	private ConfigBody onMessage;

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
			checkConfig(onMessage == null, "can only have one message handler at the moment");
			onMessage = config.body();
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(ModuleSetup module) {

		module.operation(path("send"), provider -> new IrcSendConfigurer());
		
		module.setupInitializer(init -> {
			
			init.run("create bot", store -> {
				store.put(BOT, new RekaBot(name, hostname, channel, key));
			});
			
		});

		module.onShutdown("disconnect", store -> {
			store.remove(BOT).ifPresent(RekaBot::shutdown);
		});

		if (onMessage != null) {
			module.trigger("on message", onMessage, reg -> {
				RekaBot bot = reg.store().get(BOT);
				Flow flow = reg.flow();
				bot.addListener(new IrcListener() {
					
					@Override
					public void onMessage(String channel, String sender, String login, String hostname, String message) {
						flow.prepare().data(MutableMemoryData.create()
								.putString("channel", channel)
								.putString("sender", sender)
								.putString("login", login)
								.putString("hostname", hostname)
								.putString("message", message)).run();
					}
					
					@Override
					public void onConnect() {
					}

				});
				bot.connect();
			});
		}
		
		
	}
}
