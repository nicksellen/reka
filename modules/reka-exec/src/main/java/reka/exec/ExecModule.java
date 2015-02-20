package reka.exec;

import static reka.api.Path.path;

import java.lang.reflect.Field;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;

public class ExecModule implements Module {

	static {
		try {
			Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
			field.setAccessible(true);
			field.set(null, java.lang.Boolean.FALSE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Path base() {
		return path("exec");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new ExecConfigurer());
	}

}
