package reka.builtins.adder;

import static reka.api.Path.path;

import java.util.concurrent.atomic.LongAdder;

import reka.api.IdentityKey;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class AdderConfigurer extends ModuleConfigurer {
	
	public static final IdentityKey<LongAdder> ADDER = IdentityKey.named("adder");

	@Override
	public void setup(ModuleSetup module) {
		module.setupInitializer(init -> {
			init.run("create counter", ctx -> {
				ctx.put(ADDER, new LongAdder());
			});
		});
		module.operation(path("inc"), provider -> new IncrementConfigurer());
		module.operation(path("dec"), provider -> new DecrementConfigurer());
		module.operation(path("sum"), provider -> new SumConfigurer());
	}

}
