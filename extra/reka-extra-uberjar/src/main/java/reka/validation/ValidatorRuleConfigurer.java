package reka.validation;

import static reka.api.Path.dots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import reka.api.Path;
import reka.config.Config;
import reka.configurer.annotations.Conf;

public class ValidatorRuleConfigurer implements Supplier<Collection<ValidatorRule>> {

	private final List<ValidatorRule> rules = new ArrayList<>();
	
	private Path path;
	
	private boolean required = false;
	private final List<Pattern> patterns = new ArrayList<>();
	
	@Conf.At("path")
	public void path(String val) {
		path = dots(val);
	}
	
	@Conf.At("required")
	public void required(Config config) {
		required = true;
	}
	
	@Conf.Each("pattern")
	public void pattern(String val) {
		patterns.add(Pattern.compile(val));
	}

	@Override
	public Collection<ValidatorRule> get() {
		List<ValidatorRule> all = new ArrayList<>();
		all.addAll(rules);
		if (required) {
			all.add(new PresenceValidator(path));
		}
		for (Pattern pattern : patterns) {
			rules.add(new PatternValidator(path, pattern));
		}
		return all;
	}
	
}