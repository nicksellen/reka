package reka.http;

import static reka.api.Path.path;
import reka.api.IdentityKey;
import reka.api.Path;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

public class HttpSessionsModule extends ModuleConfigurer {
	
	protected static final IdentityKey<SessionStore> SESSION_STORE = IdentityKey.named("session store");
	
	public final static String COOKIENAME = "REKASESSID";

	@Override
	public void setup(ModuleSetup module) {
		Path storePath = module.path().add("store");
		module.init(seq -> {
			seq.storeRun("create session storage", store -> {
				store.put(SESSION_STORE, new SessionStore());
			});
		});
		module.operation(path("put"), () -> new SessionPutConfigurer());
		module.operation(path("get"), () -> new SessionGetConfigurer());
		module.operation(path("remove"), () -> new SessionRemoveConfigurer());
	}

}
