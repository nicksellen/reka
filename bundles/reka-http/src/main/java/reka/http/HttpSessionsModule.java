package reka.http;

import static reka.api.Path.path;
import reka.api.Path;
import reka.api.content.Contents;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;


public class HttpSessionsModule extends ModuleConfigurer {
	
	public final static String COOKIENAME = "REKASESSID";

	@Override
	public void setup(ModuleInit module) {
		Path storePath = module.path().add("store");
		module.init("create session storage", (data) -> {
			return data.put(storePath, Contents.nonSerializableContent(new SessionStore()));
		});
		module.operation(path("put"), () -> new SessionPutConfigurer(storePath));
		module.operation(path("get"), () -> new SessionGetConfigurer(storePath));
	}

}
