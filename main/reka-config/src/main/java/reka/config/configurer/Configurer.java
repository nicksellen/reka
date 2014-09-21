package reka.config.configurer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.configurer.annotations.Conf;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class Configurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final Map<Class<?>,Configurer> configurers = new HashMap<>();
	
	private final Class<?> klass;
	private final List<ConfOption> options = new ArrayList<>();
	private final Map<Method,Conf.Deprecated> deprecatedMethods = new HashMap<>();
	private final Map<Method,Conf.Deprecated.Warning> deprecatedWarningMethods = new HashMap<>();
	
	public interface Preconditions {
	
		public static void checkConfig(boolean condition, String msg, Object... objs) {
			if (!condition) throw preconditionError(msg, objs);
		}
		
		public static void checkConfigPresent(Object val, String msg, Object... objs) {
			if (val == null) throw preconditionError(msg, objs); 
		}
		
		public static void invalidConfig(String msg, Object...objs) {
			 throw preconditionError(msg, objs);
		}
	
	}

	public static class ErrorCollector {
		
		private final ConfigOrNavigableConfig config;
		private final List<ConfigurationError> errors;
		
		private ErrorCollector(ConfigOrNavigableConfig config, List<ConfigurationError> errors) {
			this.config = config;
			this.errors = errors;
		}
		
		public void add(String msg, Object... objs) {
			errors.add(new ConfigurationError(config, format(msg, objs)));
		}

		public void checkConfigPresent(Object val, String msg, Object... objs) {
			if (val == null) add(msg, objs);
		}
		
		public void checkConfig(boolean condition, String msg, Object... objs) {
			if (!condition) add(msg, objs);
		}
	}

	private static PreconditionConfigurationException preconditionError(String msg, Object... objs) {
		return new PreconditionConfigurationException(format(msg, objs));
	}

    public static <T> T configure(T target, NavigableConfig body) {
    	return configure(target, new WrappedNavigableConfig(body)).get();
    }

    public static <T> T configure(T target, Config config) {
    	return configure(target, new WrappedConfig(config)).get();
    }
    
    @SuppressWarnings("unchecked")
	private static <T> Configured<T> configure(T target, ConfigOrNavigableConfig c) {
    	return (Configured<T>) findConfigurer(target.getClass()).apply(c, target);
    }

    private static class PreconditionConfigurationException extends RuntimeException {

		private static final long serialVersionUID = -5535432203358657347L;

		public PreconditionConfigurationException(String msg) {
			super(msg);
		}
    	
    }
    
    public static class ConfigurationError {

    	private final ConfigOrNavigableConfig config;
    	private final String msg;
    	
		private final Throwable cause;
    	
    	private ConfigurationError(ConfigOrNavigableConfig config, String msg, Throwable cause) {
    		this.config = config;
    		this.msg = msg;
    		this.cause = cause;
		}
    	
    	private ConfigurationError(ConfigOrNavigableConfig config, String msg) {
    		this(config, msg, null);
    	}
    	
    	public ConfigOrNavigableConfig config() {
    		return config;
    	}
    	
    	public String message() {
    		return msg;
    	}
    	
    	public Throwable cause() {
    		return cause;
    	}
    	
    	@Override
    	public String toString() {
    		Source source = config.source();
    		String content = source.content();
    		if (content.length() > 20) {
    			content = content.substring(0, 20) + " ...";
    		}
    		
    		return format("%s (%s) [%s]", msg, source.location(), content);
    	}
    	
    	public String toStringWithOriginalStacktrace() {
    		if (cause == null) {
    			return format("%s (no cause)", toString());
    		} else {
    			StringWriter w = new StringWriter();
    			PrintWriter pw = new PrintWriter(w);
    			rootCause(cause).printStackTrace(pw);
    			
    			return format("%s\n %s %s", 
    					toString(), 
    					allExceptionMessages(cause), 
    					w.toString());
    		}
    	}
    	
    }
    
    private static class Configured<T> {

    	@SuppressWarnings("unused")
		private final ConfigOrNavigableConfig config;
    	private final T value;
    	private final List<ConfigurationError> errors;
    	
    	Configured(ConfigOrNavigableConfig config, T value, List<ConfigurationError> errors) {
    		this.config = config;
    		this.value = value;
    		this.errors = errors;
    	}
    	
    	public boolean hasErrors() {
    		return !errors.isEmpty();
    	}
    	
    	public T get() {
    		if (hasErrors()) throw new InvalidConfigurationException(errors);
    		return value;
    	}
    	
    }
    
    public static class InvalidConfigurationException extends RuntimeException {
    	
		private static final long serialVersionUID = 8018639942600973192L;

		private final Collection<ConfigurationError> errors;
		
		InvalidConfigurationException(Collection<ConfigurationError> errors) {
    		super();
    		this.errors = errors;
		}
		
		@Override
		public String toString() {
			return format("invalid configuration:\n - %s",
					errors.stream().map(ConfigurationError::message).collect(joining("\n - ")));
		}
		
		@Override
		public String getMessage() {
			return toString();
		}
		
		public Collection<ConfigurationError> errors() {
			return errors;
		}
    	
    }
    
	public static interface ConfigOrNavigableConfig {
	    public boolean hasConfig();
	    public Config config();
	    public boolean hasBody();
	    public NavigableConfig body();
	    public Source source();
	}
	
	private static class WrappedNavigableConfig implements ConfigOrNavigableConfig {
		
	    private final NavigableConfig body;
	    
	    WrappedNavigableConfig(NavigableConfig body) {
	        this.body = body;
	    }
        
        @Override
	    public boolean hasConfig() {
	        return false;
	    }
        
        @Override
	    public Config config() {
	        return null;
	    }
        
        @Override
        public boolean hasBody() {
            return true;
        }
        
        @Override
        public NavigableConfig body() {
            return body;
        }
        
        @Override
        public Source source() {
        	return body.source();
        }
	}
	
	private static class WrappedConfig implements ConfigOrNavigableConfig {
	    
		private final Config config;
	    
	    WrappedConfig(Config config) {
	        this.config = config;
	    }
	    
	    public boolean hasConfig() {
	        return true;
	    }
        
        @Override
	    public Config config() {
	        return config;
	    }
        
        @Override
        public boolean hasBody() {
            return config.hasBody();
        }
        
        @Override
        public NavigableConfig body() {
            return config.body();
        }

        @Override
        public Source source() {
        	return config.source();
        }
	}
	
	public static Configurer findConfigurer(Class<?> klass) {
		Configurer configurer = configurers.get(klass);
		if (configurer == null) {
			configurer = new Configurer(klass);
			configurers.put(klass, configurer);
		}
		return configurer;
	}
	
	interface MethodFunction {
		void apply(Method method);
	}
	
	private void applyToClassHierarchy(MethodFunction... mfs) {
		for (MethodFunction mf : mfs) {
			Class<?> k = klass;
			while (k != null) {
				for (Method method : k.getDeclaredMethods()) mf.apply(method);
				k = k.getSuperclass();
			}
		}
	}
 	
	private Configurer(Class<?> klass) {
		
		this.klass = klass;
		
		applyToClassHierarchy(
			(method) -> configureDeprecated(method, method.getAnnotation(Conf.Deprecated.class)),
			(method) -> configureDeprecatedWarning(method, method.getAnnotation(Conf.Deprecated.Warning.class)),
			(method) -> configureKey(method, method.getAnnotation(Conf.Key.class)),
			(method) -> configureSubkey(method, method.getAnnotation(Conf.Subkey.class)),
			(method) -> configureVal(method, method.getAnnotation(Conf.Val.class)),
			(method) -> configureConfig(method, method.getAnnotation(Conf.Config.class)),
			(method) -> configureAt(method, method.getAnnotationsByType(Conf.At.class)),
			(method) -> configureEach(method, method.getAnnotationsByType(Conf.Each.class)),
			(method) -> configureEachChildOf(method, method.getAnnotationsByType(Conf.EachChildOf.class)),
			(method) -> configureEachUnmatchedKey(method, method.getAnnotation(Conf.EachUnmatched.class)));
		
		Collections.sort(options, comparing(ConfOption::order));
	}

	private Configured<Object> apply(ConfigOrNavigableConfig config, Object instance) {
		Status status = new Status();
		List<ConfigurationError> errors = new ArrayList<>();
		for (ConfOption option : options) {
			try {
			    option.apply(config, instance, status);
			} catch (Throwable t) {
				InvalidConfigurationException e = findInvalidConfigurationException(t);
				if (e != null) {
					errors.addAll(e.errors);
				} else {
					errors.add(new ConfigurationError(config, rootExceptionMessage(t), t));
				}
			}
		}
		
		if (instance instanceof ErrorReporter) {
			ErrorCollector collector = new ErrorCollector(config, errors);
			((ErrorReporter) instance).errors(collector);
		}
		
		if (config.hasBody()) {
			for (Config c : config.body()) {
				if (c.hasKey() && !status.matchedKeys.contains(c.key())) {
					errors.add(new ConfigurationError(new WrappedConfig(c), format("invalid option %s", c.key())));		
				}
			}
		}
		
		if (!errors.isEmpty()) {
			log.debug("there are {} errors! {}\n", errors.size(), errors);
		}
		
		return new Configured<Object>(config, instance, errors);
	}
	
	private static InvalidConfigurationException findInvalidConfigurationException(Throwable t) {
		while (t != null) {
			if (t instanceof InvalidConfigurationException) {
				return ((InvalidConfigurationException) t);
			}
			t = t.getCause();
		}
		return null;
	}
	
	private static String rootExceptionMessage(Throwable t) {
		Collection<String> msgs = allExceptionMessages(t);
		return msgs.isEmpty() ? "unknown" : msgs.iterator().next();
	}
	
	private static Throwable rootCause(Throwable t) {
		Throwable cause = t.getCause();
		while (cause != null) {
			t = cause;
			cause = t.getCause();
		}
		return t;
	}
	
	private static Collection<String> allExceptionMessages(Throwable original) {
		List<String> result = new ArrayList<>();
		
		Throwable t = original;
		
		while (t != null) {
			if (t.getMessage() != null) {
				result.add(t.getMessage());
			}
			t = t.getCause();
		}
		
		Collections.reverse(result);
		
		return result;
	}

	private void checkDeprecation(Method method, Config config) {
		
		Conf.Deprecated deprecated = deprecatedMethods.get(method);
		if (deprecated != null) {
			String msg = format("config key [%s] is deprecated", config.key());
			if (!"".equals(deprecated.value())) {
				msg = msg + " " + deprecated.value();
			}
			throw new InvalidConfigurationException(asList(new ConfigurationError(new WrappedConfig(config), msg)));
		}

		Conf.Deprecated.Warning warning = deprecatedWarningMethods.get(method);
		if (warning != null) {
			String msg = format("config key [%s] will be deprecated", config.key());
			if (!"".equals(warning.value())) {
				msg = msg + " - " + warning.value();
			}
			log.warn("warn: {}", msg);
		}
	}

	private static class Status {
		final Set<String> matchedKeys = new HashSet<>();
		private void addFirstAsMatched(String val) {
			matchedKeys.add(val);
			Iterator<String> it = dotSplitter.split(val).iterator();
			if (it.hasNext()) {
				matchedKeys.add(it.next());
			}
		}
	}
	
	private abstract class ConfOption {
		
		protected final Method method;
		
		ConfOption(Method method) {
			this.method = method;
		}
		
		public abstract void apply(ConfigOrNavigableConfig config, Object instance, Status status);
		public abstract int order();
	}
	
	private class EachChildOfOption extends ConfOption {
		
		private final String path;
		
		EachChildOfOption(Method method, String path) {
			super(method);
			this.path = path;
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
		    	status.addFirstAsMatched(path);
    			for (Config child : config.body().eachChildOf(path)) {
    				try {
    					checkDeprecation(method, child);
    					method.invoke(instance, child);
	                } catch (Throwable t) {
						throw asInvalidConfigurationException(child, t);
	                }
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
	}
	
	private class EachStringValOption extends ConfOption {

	    private final String match;
	    
        EachStringValOption(Method method, String match) {
            super(method);
            this.match = match;
        }

        @Override
        public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
            if (config.hasBody()) {
                if (match.equals("")) {
                    // all immediate children
                    for (Config child : config.body()) {
                        status.addFirstAsMatched(child.key());
                        try {
        					checkDeprecation(method, child);
                        	method.invoke(instance, child.valueAsString());
                        } catch (Throwable t) {
	    					throw asInvalidConfigurationException(child, t);
                        }
                    }
                } else {
                    // all children at path
                    status.addFirstAsMatched(match);
                    for (Config child : config.body().each(match)) {
                        try {
        					checkDeprecation(method, child);
                        	method.invoke(instance, child.valueAsString());
                        } catch (Throwable t) {
	    					throw asInvalidConfigurationException(child, t);
                        }
                    }
                }
            }
        }

        @Override
        public int order() {
            return 10;
        }
	    
	}
	
	private class EachOption extends ConfOption {
		
		private final String match;
		
		EachOption(Method method, String match) {
			super(method);
			this.match = match;
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
    			if (match.equals("")) {
    				for (Config child : config.body().each()) {
    					status.addFirstAsMatched(child.key());
    					try {
        					checkDeprecation(method, child);
    						method.invoke(instance, child);
		                } catch (Throwable t) {
							throw asInvalidConfigurationException(child, t);
		                }
    				}
    			} else {
					status.addFirstAsMatched(match);
    				for (Config child : config.body().each(match)) {
    					try {
        					checkDeprecation(method, child);
        					method.invoke(instance, child);
    	                } catch (Throwable t) {
    						throw asInvalidConfigurationException(child, t);
    	                }
    				}
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
	}

	private class EachUnmatchedKeyOption extends ConfOption {
		
		EachUnmatchedKeyOption(Method method) {
			super(method);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
		    	Set<String> newlymatched = new HashSet<>();
    			for (Config child : config.body()) {
    				if (!status.matchedKeys.contains(child.key())) {
    					try {
        					checkDeprecation(method, child);
        					newlymatched.add(child.key());
    						method.invoke(instance, child);
		                } catch (Throwable t) {
							throw asInvalidConfigurationException(child, t);
		                }
    				}
    		    }
    			status.matchedKeys.addAll(newlymatched); // they are matched now :)
		    }
		}

		@Override
		public int order() {
			return 20;
		}
	}
	
	private abstract class AtOption extends ConfOption {
		
		protected final String path;
		
		AtOption(Method method, String path) {
			super(method);
			this.path = path;
		}
		
		@Override
		public String toString() {
		    return format("%s(path: \"%s\")", getClass().getSimpleName(), path);
		}
		
	}
	
	private class ConfigOption extends ConfOption {
		
		ConfigOption(Method method) {
			super(method);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasConfig()) {
		    	try {
					checkDeprecation(method, config.config());
					status.matchedKeys.addAll(allKeys(config));
		    		method.invoke(instance, config.config());
                } catch (Throwable t) {
					throw asInvalidConfigurationException(config.config(), t);
                }
		    }
		}

		@Override
		public int order() {
			return 5;
		}
		
	}

	private class KeyOption extends ConfOption {
		
		KeyOption(Method method) {
			super(method);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
			if (config.hasConfig()) {
				try {
					checkDeprecation(method, config.config());
					status.matchedKeys.add(config.config().key());
					method.invoke(instance, config.config().key());
                } catch (Throwable t) {
					throw asInvalidConfigurationException(config.config(), t);
                }
			}
		}

		@Override
		public int order() {
			return 5;
		}
		
	}
	
	private class SubkeyOption extends ConfOption {
		
		SubkeyOption(Method method) {
			super(method);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
			if (config.hasConfig() && config.config().hasSubkey()) {
				try {
					checkDeprecation(method, config.config());
					status.matchedKeys.add(config.config().key());
					method.invoke(instance, config.config().subkey());
                } catch (Throwable t) {
					throw asInvalidConfigurationException(config.config(), t);
                }
			}
		}

		@Override
		public int order() {
			return 5;
		}
		
	}
		
	private class StringValOption extends ConfOption {
		
		StringValOption(Method method) {
			super(method);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
            if (config.hasConfig()) {
            	if (config.config().hasValue()) {
            		try {
    					checkDeprecation(method, config.config());
            			method.invoke(instance, config.config().valueAsString());
	                } catch (Throwable t) {
						throw asInvalidConfigurationException(config.config(), t);
	                }
            	}
            }
		}

		@Override
		public int order() {
			return 5;
		}
		
	}
	
	private static final List<String> trueValues = asList("true", "yes", "on");
	private static final List<String> falseValues = asList("false", "no", "off");
	
	private class BooleanAtOption extends AtOption {

		BooleanAtOption(Method method, String path) {
			super(method, path);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
			if (config.hasBody()) {
    		    Optional<Config> at = config.body().at(path);
    			if (at.isPresent()) {
    				Config conf = at.get();
    				
    				boolean val = true;
    				if (conf.hasValue()) {
    					status.addFirstAsMatched(path);
    					checkDeprecation(method, conf);
    					String str = conf.valueAsString();
    					
    					if (trueValues.contains(str)) {
    						val = true;
    					} else if (falseValues.contains(str)) {
    						val = false;
    					} else {
    						throw preconditionError(
    								"must be empty (true) or one of %s (true) or %s (false)", 
    								trueValues, falseValues);
    					}
    				}
    				
    				try {
    					method.invoke(instance, val);
	                } catch (Throwable t) {
						throw asInvalidConfigurationException(conf, t);
	                }
    				
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
		
	}
	
	private class IntegerAtOption extends AtOption {
		
		IntegerAtOption(Method method, String path) {
			super(method, path);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
    		    Optional<Config> at = config.body().at(path);
    			if (at.isPresent()) {
    				Config conf = at.get();
    				if (conf.hasValue()) {
    					status.addFirstAsMatched(path);
	    				try {
	    					checkDeprecation(method, conf);
	    					method.invoke(instance, conf.valueAsBigDecimal());
		                } catch (Throwable t) {
		                	t.printStackTrace();
							throw asInvalidConfigurationException(conf, t);
		                }
    				}
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
	}
	
	private static final Splitter dotSplitter = Splitter.on(".");
	

	private class StringAtOption extends AtOption {
		
		StringAtOption(Method method, String path) {
			super(method, path);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
    		    Optional<Config> at = config.body().at(path);
    			if (at.isPresent()) {
    				Config conf = at.get();
    				if (conf.hasValue()) {
    					status.addFirstAsMatched(path);
	    				try {
	    					checkDeprecation(method, conf);
	    					method.invoke(instance, conf.valueAsString());
	    				} catch (Throwable t) {
	    					throw asInvalidConfigurationException(conf, t);
	    				}
    				}
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
	}
	
	private class ConfigAtOption extends AtOption {
		
		ConfigAtOption(Method method, String path) {
			super(method, path);
		}

		@Override
		public void apply(ConfigOrNavigableConfig config, Object instance, Status status) {
		    if (config.hasBody()) {
    			Optional<Config> at = config.body().at(path);
    			if (at.isPresent()) {
    				Config conf = at.get();
    				try {
	    				status.addFirstAsMatched(path);
    					checkDeprecation(method, conf);
	    				method.invoke(instance, conf);
    				} catch (Throwable t) {
    					throw asInvalidConfigurationException(conf, t);
    				}
    			}
		    }
		}

		@Override
		public int order() {
			return 10;
		}
	}
	
	private static InvalidConfigurationException asInvalidConfigurationException(Config conf, Throwable t) {
		t.printStackTrace();
		InvalidConfigurationException e = findInvalidConfigurationException(t);
		if (e != null) return e;
		return new InvalidConfigurationException(asList(new ConfigurationError(new WrappedConfig(conf), rootExceptionMessage(t),t)));
	}
	
	private void configureEachChildOf(Method method, Conf.EachChildOf[] annotations) {
		if (annotations == null || annotations.length == 0) return;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Config.class), 
			"@%s %s must accept a single %s parameter",
				Conf.EachChildOf.class.getSimpleName(),
				method, 
				Config.class.getSimpleName());
		
		for (Conf.EachChildOf annotation : annotations) {
			options.add(new EachChildOfOption(method, annotation.value()));
		}
	}
	
	private void configureEach(Method method, Conf.Each[] annotations) {
		if (annotations == null || annotations.length == 0) return;
		checkArgument(method.getParameterTypes().length == 1 && (
		        method.getParameterTypes()[0].equals(Config.class) ||
		        method.getParameterTypes()[0].equals(String.class)
		     ), 
			"@%s %s must accept a single %s or %s parameter",
				Conf.Each.class.getSimpleName(),
				method, 
				Config.class.getSimpleName(),
				String.class.getSimpleName());
		
		Class<?> type = method.getParameterTypes()[0];
		
		for (Conf.Each annotation : annotations) {
			if (type.equals(Config.class)) {
		        options.add(new EachOption(method, annotation.value()));
			} else if (type.equals(String.class)) {
		        options.add(new EachStringValOption(method, annotation.value()));
			}		
		}
	}
	
	private void configureEachUnmatchedKey(Method method, Conf.EachUnmatched annotation) {
		if (annotation == null) return;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Config.class), 
			"@%s %s must accept a single %s parameter",
				Conf.EachChildOf.class.getSimpleName(),
				method, 
				Config.class.getSimpleName());
		
		options.add(new EachUnmatchedKeyOption(method));
	}

	private void configureConfig(Method method, Conf.Config annotation) {
		if (annotation == null) return;
		Class<?> requiredClass = Config.class;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(requiredClass), 
				"@%s %s must accept a single %s parameter",
					Conf.Config.class.getSimpleName(),
					method, 
					requiredClass.getSimpleName());
		
		options.add(new ConfigOption(method));
	}

	private void configureDeprecated(Method method, Conf.Deprecated annotation) {
		if (annotation == null) return;
		deprecatedMethods.put(method, annotation);
	}
	
	private void configureDeprecatedWarning(Method method, Conf.Deprecated.Warning annotation) {
		if (annotation == null) return;
		deprecatedWarningMethods.put(method, annotation);
	}
	
	private void configureKey(Method method, Conf.Key annotation) {
		if (annotation == null) return;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class), 
				"@%s %s must accept a single %s parameter",
					Conf.Key.class.getSimpleName(),
					method, 
					String.class.getSimpleName());
		
		options.add(new KeyOption(method));
	}
	
	private void configureSubkey(Method method, Conf.Subkey annotation) {
		if (annotation == null) return;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class), 
				"@%s %s must accept a single %s parameter",
					Conf.Subkey.class.getSimpleName(),
					method, 
					String.class.getSimpleName());
		
		options.add(new SubkeyOption(method));
	}

	private void configureVal(Method method, Conf.Val annotation) {
		if (annotation == null) return;
		checkArgument(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class), 
				"@%s %s must accept a single %s parameter",
					Conf.Key.class.getSimpleName(),
					method, 
					String.class.getSimpleName());
		
		options.add(new StringValOption(method));
	}

	private void configureAt(Method method, Conf.At[] annotations) {
		if (annotations == null || annotations.length == 0) return;
		checkArgument(method.getParameterTypes().length == 1, 
				"@%s %s must accept a single parameter",
					Conf.At.class.getSimpleName(),
					method);
		
		for (Conf.At annotation : annotations) {
			Class<?> valueClass = method.getParameterTypes()[0];
			
			if (Number.class.isAssignableFrom(valueClass)) {
				options.add(new IntegerAtOption(method, annotation.value()));
			} else if (Boolean.class.isAssignableFrom(valueClass)) {
				options.add(new BooleanAtOption(method, annotation.value()));
			} else if (valueClass.equals(String.class)) {
				options.add(new StringAtOption(method, annotation.value()));
			} else if (valueClass.equals(Config.class)) {
				options.add(new ConfigAtOption(method, annotation.value()));
			} else {
				throw new RuntimeException(format("@%s %s - don't know how to extract %s value from Config", 
						Conf.At.class.getSimpleName(),
						method, valueClass.getSimpleName()));
			}
		}
	}

	private static Set<String> allKeys(ConfigOrNavigableConfig config) {
		if (config.hasBody()) {
			return asList(Iterables.toArray(config.body(), Config.class)).stream()
					.filter(Config::hasKey)
					.map(Config::key)
					.collect(toSet());
		} else {
			return Collections.emptySet();
		}
	}
	
}
