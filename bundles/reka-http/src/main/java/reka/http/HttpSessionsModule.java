package reka.http;

import static reka.api.Path.path;
import reka.api.IdentityKey;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

public class HttpSessionsModule extends ModuleConfigurer {
	
	protected static final IdentityKey<SessionStore> SESSION_STORE = IdentityKey.named("session store");
	
	public final static String COOKIENAME = "REKASESSID";

	@Override
	public void setup(ModuleSetup module) {
		module.setupInitializer(seq -> {
			seq.run("create session storage", store -> {
				store.put(SESSION_STORE, new SessionStore());
			});
		});
		module.operation(path("put"), provider -> new SessionPutConfigurer());
		module.operation(path("get"), provider -> new SessionGetConfigurer());
		module.operation(path("remove"), provider -> new SessionRemoveConfigurer());
	}

}
