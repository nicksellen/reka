package reka.http;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;

import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;

public class WebsocketTopicConfigurer {
	
	private final IdentityKey<Object> key;
	
	private final List<ConfigBody> onMessage = new ArrayList<>();
	
	public WebsocketTopicConfigurer(IdentityKey<Object> key) {
		this.key = key;
	}
	
	public IdentityKey<Object> key() {
		return key;
	}
	
	public List<ConfigBody> onMessage() {
		return onMessage;
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		checkConfig(config.hasBody(), "must have a body");
		switch (config.valueAsString()) {
		case "message":
			onMessage.add(config.body());
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

}
