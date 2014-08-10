package reka.mustache;

import static java.util.Arrays.asList;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseMustache extends UseConfigurer {

	@Override
	public void setup(UseInit init) {
		init.operation(asList("render", ""), () -> new MustacheConfigurer());
	}

}
