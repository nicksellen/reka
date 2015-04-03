package reka.modules.builtins.adder;

import static reka.util.Path.path;

import java.util.concurrent.atomic.LongAdder;

import reka.identity.IdentityKey;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class AdderConfigurer extends ModuleConfigurer {
	
	public static final IdentityKey<LongAdder> ADDER = IdentityKey.named("adder");

	@Override
	public void setup(AppSetup app) {
		app.onDeploy(init -> {
			init.run("create counter", () -> {
				app.ctx().put(ADDER, new LongAdder());
			});
		});
		app.defineOperation(path("inc"), provider -> new IncrementConfigurer());
		app.defineOperation(path("dec"), provider -> new DecrementConfigurer());
		app.defineOperation(path("sum"), provider -> new SumConfigurer());
	}

}
