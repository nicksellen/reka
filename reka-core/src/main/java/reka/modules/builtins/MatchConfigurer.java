package reka.modules.builtins;

import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import reka.api.Path;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.RouteCollector;
import reka.flow.ops.RouteKey;
import reka.flow.ops.RouterOperation;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;

public class MatchConfigurer implements OperationConfigurer {
	
	public static interface MatchThing {
		boolean matches(String input, MutableData data);
	}

	public static class ExactValueMatcher implements MatchThing {
		
		private final String value;
		
		public ExactValueMatcher(String value) {
			this.value = value;
		}
		
		@Override
		public boolean matches(String input, MutableData data) {
			return input.equals(value);
		}
		
	}

	public static class RegexValueMatcher implements MatchThing {
		
		private final Pattern pattern;
		private final Path dest = path("matches");
		
		public RegexValueMatcher(Pattern pattern) {
			this.pattern = pattern;
		}
		
		@Override
		public boolean matches(String input, MutableData data) {
			java.util.regex.Matcher match = pattern.matcher(input);
			if (!match.find()) return false;
			data.putString(dest.add(0), match.group());
			for (int i = 1; i <= match.groupCount(); i++) {
				data.putString(dest.add(i), match.group(i));
			}
			return true;
		}
		
	}
	
	public static class Matcher {
		
		private final RouteKey key;
		private final MatchThing matcher;
		
		public Matcher(RouteKey key, MatchThing matcher) {
			this.key = key;
			this.matcher = matcher;
		}
		
		public RouteKey key() {
			return key;
		}
		
		public MatchThing matcher() {
			return matcher;
		}
		
	}
	
	private final ConfigurerProvider provider;
	
	private Function<Data,String> matchFn;
	private final List<Matcher> matchers = new ArrayList<>();
	private final Map<RouteKey,ConfigBody> bodies = new HashMap<>();
	private RouteKey otherwise;

	public MatchConfigurer(ConfigurerProvider provider) {
		this.provider = provider;
	}
	
	@Conf.Val
	public void match(String val) {
		matchFn = StringWithVars.compile(val);
	}
	
	@Conf.Each("when")
	public void when(Config config) {
		checkConfig(config.hasValue(), "must provide a value to match against");
		String val = config.valueAsString();
		addMatcher(val, new ExactValueMatcher(val), config.body());
	}
	
	@Conf.Each("when/re")
	public void whenRe(Config config) {
		checkConfig(config.hasValue(), "must provide a regex value to match against");
		String val = config.valueAsString();
		addMatcher(val, new RegexValueMatcher(Pattern.compile(val)), config.body());
	}
	
	private void addMatcher(String name, MatchThing matcher, ConfigBody body) {
		RouteKey key = RouteKey.named(name);
		matchers.add(new Matcher(key, matcher));
		bodies.put(key, body);
	}
	
	@Conf.At("otherwise")
	public void otherwise(Config config) {
		RouteKey key = RouteKey.named("otherwise");
		otherwise = key;
		bodies.put(key, config.body());
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.router("match", () -> new MatcherOperation(matchFn, matchers, otherwise), routes -> {
			bodies.forEach((key, body) -> {
				routes.add(key, configure(new SequenceConfigurer(provider), body));
			});
		});
	}
	
	public static class MatcherOperation implements RouterOperation {
		
		private final Function<Data,String> matchFn;
		private final List<Matcher> matchers;
		private final RouteKey otherwise;
		
		public MatcherOperation(Function<Data,String> matchFn, List<Matcher> matchers, RouteKey otherwise) {
			this.matchFn = matchFn;
			this.matchers = matchers;
			this.otherwise = otherwise;
		}

		@Override
		public void call(MutableData data, RouteCollector router) {
			String val = matchFn.apply(data);
			Optional<Matcher> matcher = matchers.stream().filter(m -> m.matcher().matches(val, data)).findFirst();
			if (matcher.isPresent()) {
				router.routeTo(matcher.get().key());
			} else if (otherwise != null) {
				router.routeTo(otherwise);
			}
			
		}
		
	}

}
