package reka;

import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseRoot extends UseConfigurer {
	
	public UseRoot() {
		type("root").isRoot(true);
	}

	@Override
	public void setup(UseInit register) { }

}
