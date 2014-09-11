package reka.http.operations;

import static reka.config.configurer.Configurer.configure;

import java.util.HashMap;
import java.util.Map;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationSetup;
import reka.nashorn.OperationConfigurer;

import com.google.common.collect.ImmutableMap;

public class BasicAuthConfigurer implements OperationConfigurer {
	
	private final ConfigurerProvider provider;
	private final Map<String,String> credentials = new HashMap<>();
	
	private String realm;
	private OperationConfigurer ok, fail;
	
	public BasicAuthConfigurer(ConfigurerProvider provider) {
		this.provider = provider;
	}
	
	@Conf.At("realm")
	public void realm(String val) {
		realm = val;
	}
	
	@Conf.EachChildOf("accounts")
	public void creds(Config config) {
		credentials.put(config.key(), config.valueAsString());
	}
	
	@Conf.At("ok")
	public void ok(Config config) {
		ok = ops(config.body());
	}
	
	@Conf.At("fail")
	public void fail(Config config) {
		fail = ops(config.body());
	}
	
	private OperationConfigurer ops(ConfigBody body) {
		return configure(new SequenceConfigurer(provider), body);
	}

	@Override
	public void setup(OperationSetup ops) {
		CredentialsChecker checker = new MapCredentialsChecker(ImmutableMap.copyOf(credentials));
		
		ops.router("basic-auth", store -> new BasicAuthRouter(realm, checker), router -> {
			router.add("ok", ok);
			router.add("fail", fail);
		});
		
	}

}
