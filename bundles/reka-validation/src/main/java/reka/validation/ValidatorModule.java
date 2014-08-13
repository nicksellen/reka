package reka.validation;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;

import java.util.ArrayList;
import java.util.List;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class ValidatorModule extends ModuleConfigurer {

	private final List<ValidatorRule> rules = new ArrayList<>();
	
	@Conf.Each("rule")
	public void rule(Config config) {
		rules.addAll(configure(new ValidatorRuleConfigurer(), config).get());
	}

	@Override
	public void setup(ModuleInit init) {
    	init.operation(path("validate"), (provider) -> new ValidateConfigurer(provider, rules));
	}

}
