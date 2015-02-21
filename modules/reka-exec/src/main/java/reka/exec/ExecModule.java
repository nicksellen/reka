package reka.exec;

import static reka.api.Path.path;
import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class ExecModule implements Module {

	/*
	private static final Logger log = LoggerFactory.getLogger(ExecModule.class);
	static {	
		try {
			Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
			field.setAccessible(true);
			field.set(null, java.lang.Boolean.FALSE);
		} catch (Throwable ex) {
			ex.printStackTrace();
			log.error("error doing something with jce", ex);
		}
	}
	*/

	@Override
	public Path base() {
		return path("exec");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new ExecConfigurer());
	}

}
