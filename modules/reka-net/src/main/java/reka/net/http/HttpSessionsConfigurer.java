package reka.net.http;

import static reka.api.Path.path;
import reka.api.IdentityKey;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class HttpSessionsConfigurer extends ModuleConfigurer {
	
	protected static final IdentityKey<SessionStore> SESSION_STORE = IdentityKey.named("session store");
	
	public final static String COOKIENAME = "REKASESSID";

	@Override
	public void setup(ModuleSetup app) {
		
		app.onDeploy(init -> {
			init.run("create session storage", () -> {
				app.ctx().calculateIfAbsent(SESSION_STORE, () -> new SessionStore());
			});
		});
		
		app.defineOperation(path("put"), provider -> new SessionPutConfigurer());
		app.defineOperation(path("get"), provider -> new SessionGetConfigurer());
		app.defineOperation(path("remove"), provider -> new SessionRemoveConfigurer());
		
	}

}
