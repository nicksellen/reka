package reka.builtins;

import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.RouteCollector;
import reka.api.run.RouteKey;
import reka.api.run.RouterOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

public class MatchConfigurer implements OperationConfigurer {

	public static class ExactValueMatcher implements Predicate<String> {
		
		private final String value;
		
		public ExactValueMatcher(String value) {
			this.value = value;
		}
		
		@Override
		public boolean test(String input) {
			return input.equals(value);
		}
		
	}
	
	public static class Matcher {
		
		private final RouteKey key;
		private final Predicate<String> matcher;
		
		public Matcher(RouteKey key, Predicate<String> matcher) {
			this.key = key;
			this.matcher = matcher;
		}
		
		public RouteKey key() {
			return key;
		}
		
		public Predicate<String> matcher() {
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
	
	private void addMatcher(String name, Predicate<String> matcher, ConfigBody body) {
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
		ops.router("match", store -> new MatcherOperation(matchFn, matchers, otherwise), routes -> {
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
			Optional<Matcher> matcher = matchers.stream().filter(m -> m.matcher().test(val)).findFirst();
			if (matcher.isPresent()) {
				router.routeTo(matcher.get().key());
			} else if (otherwise != null) {
				router.routeTo(otherwise);
			}
			
		}
		
	}

}
