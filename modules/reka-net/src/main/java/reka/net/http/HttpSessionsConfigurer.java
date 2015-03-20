package reka.net.http;

import static reka.api.Path.path;
import reka.api.IdentityKey;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class HttpSessionsConfigurer extends ModuleConfigurer {
	
	protected static final IdentityKey<SessionStore> SESSION_STORE = IdentityKey.named("session store");
	
	public final static String COOKIENAME = "REKASESSID";

	@Override
	public void setup(ModuleSetup module) {
		
		module.setupInitializer(init -> {
			init.run("create session storage", ctx -> {
				ctx.put(SESSION_STORE, new SessionStore());
			});
		});
		
		module.operation(path("put"), provider -> new SessionPutConfigurer());
		module.operation(path("get"), provider -> new SessionGetConfigurer());
		module.operation(path("remove"), provider -> new SessionRemoveConfigurer());
		
	}

}
