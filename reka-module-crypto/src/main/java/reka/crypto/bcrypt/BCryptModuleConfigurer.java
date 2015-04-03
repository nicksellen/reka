package reka.crypto.bcrypt;

import static reka.api.Path.path;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class BCryptModuleConfigurer extends ModuleConfigurer {

	@Override
	public void setup(AppSetup module) {
    	module.defineOperation(path("hashpw"), provider -> new BCryptHashpwConfigurer());
    	module.defineOperation(path("checkpw"), provider -> new BCryptCheckpwConfigurer(provider));
	}

}
