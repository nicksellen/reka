package reka.jade;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class JadeModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleInit init) {
		init.operation(asList(path("render"), root()), () -> new JadeConfigurer());
	}

}
