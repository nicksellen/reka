package reka.jade;

import static java.util.Arrays.asList;
import static reka.api.Path.path;
import static reka.api.Path.root;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseJade extends UseConfigurer {

	@Override
	public void setup(UseInit init) {
		init.operation(asList(path("render"), root()), () -> new JadeConfigurer());
	}

}
