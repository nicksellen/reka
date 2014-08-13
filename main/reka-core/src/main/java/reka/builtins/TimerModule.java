package reka.builtins;

import static java.lang.String.format;
import static reka.util.Util.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reka.api.flow.Flow;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;

public class TimerModule extends ModuleConfigurer {
	
	private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	
	private final List<Config> every = new ArrayList<>();
	
	@Conf.Each("every")
	public void every(Config config) {
		parseMs(config.valueAsString());
		every.add(config);
	}
	
	public static class TimerRun implements Runnable {
		
		private final Flow flow;
		
		public TimerRun(Flow flow) {
			this.flow = flow;
		}

		@Override
		public void run() {
			flow.run();
		}
		
	}
	
	@Override
	public void setup(ModuleInit use) {
		every.forEach(config -> {
			use.trigger(format("every %s", config.valueAsString()), config.body(), register -> {
				ScheduledFuture<?> f = executor.scheduleAtFixedRate(new TimerRun(register.flow()), 0, parseMs(config.valueAsString()), TimeUnit.MILLISECONDS);
				register.undeploy(() -> {
					f.cancel(false);
				});
			});
		});
	}
	
	private static final List<Function<String,Optional<Long>>> conversions = new ArrayList<>();
	private static class PatternConverter implements Function<String,Optional<Long>> {
		private final Pattern pattern;
		private final Function<Matcher,Long> f;
		public PatternConverter(String regex, Function<Matcher,Long> f) {
			this.pattern = Pattern.compile(regex);
			this.f = f;
		}
		@Override
		public Optional<Long> apply(String val) {
			Matcher m = pattern.matcher(val);
			if (!m.matches()) return Optional.empty();
			return Optional.of(f.apply(m));
		}
	}
	
	static {
		conversions.add(new PatternConverter("^([0-9]+(?:\\.[0-9]+)?)\\ +(?:seconds?|s)$", m -> (long) (Double.valueOf(m.group(1)) * 1000)));
		conversions.add(new PatternConverter("^([0-9]+(?:\\.[0-9]+)?)\\ +(?:minutes?|m)$", m -> (long) (Double.valueOf(m.group(1)) * 1000 * 60)));
		conversions.add(new PatternConverter("^([0-9]+)\\ +(?:milliseconds?|ms)$", m -> Double.valueOf(m.group(1)).longValue()));
	}
	
	private static long parseMs(String val) {
		for (Function<String, Optional<Long>> c : conversions) {
			Optional<Long> o = c.apply(val);
			if (o.isPresent()) {
				return o.get();
			}
		}
		throw runtime("don't know how to interpret '%s' as a time period", val);
	}

}
