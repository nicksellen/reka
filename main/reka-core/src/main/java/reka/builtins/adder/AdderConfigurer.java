package reka.builtins.adder;

import static reka.api.Path.path;

import java.util.concurrent.atomic.LongAdder;

import reka.api.IdentityKey;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class AdderConfigurer extends ModuleConfigurer {
	
	public static final IdentityKey<LongAdder> ADDER = IdentityKey.named("adder");

	@Override
	public void setup(ModuleSetup app) {
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
