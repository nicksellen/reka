package reka.crypto.bcrypt;

import static reka.api.Path.path;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;

public class BCryptModuleConfigurer extends ModuleConfigurer {

	@Override
	public void setup(AppSetup module) {
    	module.defineOperation(path("hashpw"), provider -> new BCryptHashpwConfigurer());
    	module.defineOperation(path("checkpw"), provider -> new BCryptCheckpwConfigurer(provider));
	}

}
