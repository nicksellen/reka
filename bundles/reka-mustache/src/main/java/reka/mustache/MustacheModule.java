package reka.mustache;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

public class MustacheModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup init) {
		init.operation(asList(path("render"), root()), () -> new MustacheConfigurer());
	}

}
