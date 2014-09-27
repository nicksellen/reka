package reka.common.bcrypt;

import static reka.api.Path.path;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class BCryptModule extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup module) {
    	module.operation(path("hashpw"), provider -> new BCryptHashpwConfigurer());
    	module.operation(path("checkpw"), provider -> new BCryptCheckpwConfigurer(provider));
	}

}
