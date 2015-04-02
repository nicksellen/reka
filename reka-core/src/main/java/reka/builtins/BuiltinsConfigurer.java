package reka.builtins;

import static java.lang.String.format;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.slashes;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.config.configurer.Configurer.Preconditions.invalidConfig;
import static reka.core.builder.FlowSegments.createHalt;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.FlowReferenceConfigurer;
import reka.api.Path;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BuiltinsConfigurer extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(BuiltinsConfigurer.class);

	@Override
	public void setup(AppSetup module) {
		
		module.defineOperation(path("put"), provider -> new PutConfigurer());
		module.defineOperation(path("putv"), provider -> new PutWithVarsConfigurer());
		module.defineOperation(path("copy"), provider -> new CopyConfigurer());
    	module.defineOperation(path("run"), provider -> new RunConfigurer(provider));
    	module.defineOperation(path("runp"), provider -> new RunParallelConfigurer(provider));
    	module.defineOperation(path("context"), provider -> new NewContextConfigurer(provider));
    	module.defineOperation(path("then"), provider -> new RunConfigurer(provider));
    	module.defineOperation(path("log"), provider -> new LogConfigurer());
    	module.defineOperation(path("sleep"), provider -> new SleepConfigurer());
    	module.defineOperation(path("halt!"), provider -> new HaltConfigurer());
    	module.defineOperation(slashes("uuid/generate"), provider -> new GenerateUUIDConfigurer());
    	module.defineOperation(path("println"), provider -> new PrintlnConfigurer());
    	module.defineOperation(path("uppercase"), provider -> new UppercaseConfigurer());
    	module.defineOperation(path("lowercase"), provider -> new LowercaseConfigurer());
    	
    	module.defineOperation(path("throw"), provider -> new ThrowConfigurer());
    	module.defineOperation(path("inspect"), provider -> new InspectConfigurer());
    	module.defineOperation(slashes("random/string"), provider -> new RandomStringConfigurer());
    	module.defineOperation(path("coerce"), provider -> new Coercion.CoerceConfigurer());
    	module.defineOperation(slashes("coerce/int64"), provider -> new Coercion.CoerceLongConfigurer());
    	module.defineOperation(slashes("coerce/bool"), provider -> new Coercion.CoerceBooleanConfigurer());
    	module.defineOperation(path("unzip"), provider -> new UnzipConfigurer());
    	module.defineOperation(path("split"), provider -> new SplitConfigurer());
    	
    	module.defineOperation(path("match"), provider -> new MatchConfigurer(provider));
    	
    	module.defineOperation(path("simple-uppercase"), simpleOperation(config -> {
    		Path path = dots(config.valueAsString());
    		return (data, ctx) -> {
    			data.getContent(path).ifPresent(content -> {
    				data.putString(path, content.toString().toUpperCase());
    			});
    		};
    	}));
    	
    	
	}
	
	public static class SplitConfigurer implements OperationConfigurer, ErrorReporter {
		
		private Function<Data,Path> inFn, outFn;
		private String on = ",";
		
		@Conf.Val
		@Conf.At("from")
		public void in(String val) {
			inFn = StringWithVars.compile(val).andThen(s -> dots(s));
			if (outFn == null) outFn = inFn;
		}

		@Conf.At("into")
		public void out(String val) {
			outFn = StringWithVars.compile(val).andThen(s -> dots(s));
		}
		
		@Conf.At("on")
		public void on(String val) {
			on = val;
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(inFn, "in is required");
			errors.checkConfigPresent(outFn, "out is required");
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("split", () -> new SplitOperation(inFn, outFn, Splitter.on(on)));
		}
		
	}
	
	public static class SplitOperation implements Operation {
		
		private final Splitter splitter;
		private final Function<Data,Path> inFn, outFn;
		
		public SplitOperation(Function<Data,Path> inFn, Function<Data,Path> outFn, Splitter splitter) {
			this.inFn = inFn;
			this.outFn = outFn;
			this.splitter = splitter;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			Data val = data.at(inFn.apply(data));
			if (val.isContent()) {
				data.putList(outFn.apply(data), list -> {
					for (String s : splitter.split(val.content().asUTF8())) {
						list.addString(s);
					}
				});
			}
		}
		
	}
	
	private static final Random RANDOM = new Random();
	
	public static class InspectConfigurer implements OperationConfigurer {

		private Path at = Path.root();

		@Conf.Val
		public void at(String val) {
			at = dots(val);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("inspect", () -> new InspectOperation(at));
		}
		
	}
	
	public static class InspectOperation implements Operation {

		private final Path at;

		public InspectOperation(Path at) {
			this.at = at;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			log.info(data.at(at).toPrettyJson());
		}
		
	}
	
	public static class RandomStringConfigurer implements OperationConfigurer { 

		private static final char[] chars;
		
		static {
			chars = "abcdefghijklmnopqrstuvwzyzABCDEFGHIJKLMNOPQRSTUVQXYZ0123456789".toCharArray();
		}
		
		private int length = 12;
		private Path out = Path.Response.CONTENT;
		
		@Conf.Val
		@Conf.At("length")
		public void length(String val) {
			length = Integer.valueOf(val);
		}
		
		@Conf.Val
		@Conf.At("out")
		@Conf.At("into")
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("random/string", () -> new RandomStringOperation(length, chars, RANDOM, out));
		}
		
	}
	
	public static class RandomStringOperation implements Operation {

		private final int length;
		private final char[] chars;
		private final Random random;
		private final Path out;
		
		public RandomStringOperation(int length, char[] chars, Random random, Path out) {
			this.length = length;
			this.chars = chars;
			this.random = random;
			this.out = out;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			char[] buf = new char[length];
			
			for (int i = 0; i < length; i++) {
				buf[i] = chars[random.nextInt(chars.length)];
			}
			
			data.putString(out, new String(buf));
		}
		
	}
	
	public static class ThrowConfigurer implements OperationConfigurer {
		
		private Function<Data,String> msgFn = (data) -> "error";
		
		@Conf.Val
		public void msg(String val) {
			msgFn = StringWithVars.compile(val);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("throw", () -> new ThrowOperation(msgFn));
		}
		
	}
	
	public static class PrintlnConfigurer implements OperationConfigurer {

		private Function<Data,String> msg = (data) -> "";
		
		@Conf.Config
		public void msg(Config config) {
			if (config.hasDocument()) {
				msg = StringWithVars.compile(config.documentContentAsString());
			} else if (config.hasValue()) {
				msg = StringWithVars.compile(config.valueAsString());
			}
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("println", () -> new PrintlnOperation(msg));
		}
		
	}
	
	public static class PrintlnOperation implements Operation {

		private final Function<Data,String> msg;
		
		public PrintlnOperation(Function<Data,String> msg) {
			this.msg = msg;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			System.out.println(msg.apply(data));
		}
		
	}
	
	public static class UppercaseConfigurer implements OperationConfigurer {
		
		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("uppercase", () -> new UppercaseOperation(path));
		}
		
	}
	
	public static class UppercaseOperation implements Operation {
		
		private final Path path;
		
		public UppercaseOperation(Path path) {
			this.path = path;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.getContent(path).ifPresent(content -> {
				data.putString(path, content.asUTF8().toUpperCase());
			});
		}
		
	}
	
	public static Function<ConfigurerProvider,OperationConfigurer> simpleOperation(Function<Config,Operation> fn) {
		return provider -> new SimpleOperationThing(fn);
	}
	
	public static class SimpleOperationThing implements OperationConfigurer {
		
		private final Function<Config,Operation> fn;
		
		private Config config;
		
		public SimpleOperationThing(Function<Config,Operation> fn) {
			this.fn = fn;
		}
		
		@Conf.Config
		public void config(Config config) {
			this.config = config;
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("op", () -> fn.apply(config));
		}
		
	}

	
	public static class LowercaseConfigurer implements OperationConfigurer {
		
		private Path path;
		
		@Conf.Val
		public void path(String val) {
			path = dots(val);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("uppercase", () -> new LowercaseOperation(path));
		}
		
	}
	
	public static class LowercaseOperation implements Operation {
		
		private final Path path;
		
		public LowercaseOperation(Path path) {
			this.path = path;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.getContent(path).ifPresent(content -> {
				data.putString(path, content.asUTF8().toLowerCase());
			});
		}
		
	}
	
	public static class GenerateUUIDConfigurer implements OperationConfigurer, ErrorReporter {

		private Path out = dots("uuid");
		
		@Conf.Val
		public void out(String val) {
			if (val.startsWith(":")) val = val.substring(1);
			out = dots(val);
		}

		@Override
		public void errors(ErrorCollector errors) {
			errors.checkConfigPresent(out, "please specify a value to tell us where to write the uuid, e.g. :params.uuid");
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("uuid/generate", () -> new GenerateUUIDOperation(out));
		}
		
	}
	
	public static class GenerateUUIDOperation implements Operation {

		private final Path out;
		
		public GenerateUUIDOperation(Path out) {
			this.out = out;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.putString(out, UUID.randomUUID().toString());
		}
		
	}
	
	public static class SleepConfigurer implements OperationConfigurer {

		private static final Pattern re = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(ms|m|s)?$");
		
		private long ms = 1000L;
		
		@Conf.Val
		public void timeout(String val) {
			Matcher m = re.matcher(val);
			if (!m.find()) invalidConfig("invalid sleep pattern, try something like 2.2s or 850ms");
			String num = m.group(1);
			String unit = m.group(2);
			if (unit == null) unit = "ms";
			if (num.contains(".")) {
				double n = Double.valueOf(num);
				switch (unit) {
				case "m":
					ms = Math.round(n * 1000 * 60);
					break;
				case "s":
					ms = Math.round(n * 1000);
					break;
				default:
					ms = Math.round(n);
				}
			} else {
				long n = Long.valueOf(num);
				switch (unit) {
				case "m":
					ms = n * 1000 * 60;
					break;
				case "s":
					ms = n * 1000;
					break;
				default:
					ms = n;
				}
			}
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("sleep", () -> new SleepOperation(ms));
		}
		
	}
	
	public static class SleepOperation implements AsyncOperation {

		// shared for all sleep operations
		private static final ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
		
		private final long ms;
		
		public SleepOperation(long ms) {
			this.ms = ms;
		}

		@Override
		public void call(MutableData data, OperationContext ctx, OperationResult res) {
			e.schedule(() -> res.done(), ms, TimeUnit.MILLISECONDS);
		}
		
	}
	
	public static class RunParallelConfigurer implements OperationConfigurer {
		
		private final ConfigurerProvider provider;
		
		private final List<OperationConfigurer> items = new ArrayList<>();
		
		private String label;
		
		public RunParallelConfigurer(ConfigurerProvider provider) {
			this.provider = provider;
		}
		
		@Conf.Val
		public void label(String val) {
			label = val;
		}
		
		@Conf.Each
		public void item(Config config) {
			log.debug("configuring parallel: {}", config.key());
			items.add(configure(new SequenceConfigurer(provider), ConfigBody.of(config.source(), config)));
		}

		@Override
		public void setup(OperationSetup ops) {
			if (label != null) ops.label(label);
			ops.parallel(par -> {
				items.forEach(item -> par.add(item));
			});
		}
		
	}
	
	public static class NewContextConfigurer implements OperationConfigurer {
		
		private final ConfigurerProvider provider;

		private OperationConfigurer configurer;
		private String label;
		
		public NewContextConfigurer(ConfigurerProvider provider) {
			this.provider = provider;
		}
		
		@Conf.Val
		public void label(String val) {
			label = val;
		}
		
		@Conf.Config
		public void config(Config config) {
			configurer = configure(new SequenceConfigurer(provider), config.body());
		}

		@Override
		public void setup(OperationSetup ops) {
			if (label != null) ops.label(label);
			ops.useNewContext();
			configurer.setup(ops);
		}
		
	}
	
	public static class HaltConfigurer implements OperationConfigurer {

		@Override
		public void setup(OperationSetup ops) {
			ops.add(() ->  createHalt());
		}
		
	}
	
	public static class StringWithVariablesConfigurer implements OperationConfigurer {

		private Function<Data,String> template;
		private Path out = Response.CONTENT;
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasValue()) {
				template = StringWithVars.compile(config.valueAsString());
			} else if (config.hasDocument()) {
				template = StringWithVars.compile(config.documentContentAsString());
				if (config.hasValue()) {
					out = dots(config.valueAsString());
				}
			}
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("stringwithvariables", () -> new DataContentFunctionOperation(template, out));
		}
		
	}
	
	public static class DataContentFunctionOperation implements Operation {
		
		private final Function<Data,String> template;
		private final Path out;
		
		public DataContentFunctionOperation(Function<Data,String> template, Path out) {
			this.template = template;
			this.out = out;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.putString(out, template.apply(data));
		}
		
	}
	
	public static class RunConfigurer implements OperationConfigurer {
		
		private final ConfigurerProvider provider;
		private OperationConfigurer configurer;
		
		public RunConfigurer(ConfigurerProvider provider) {
			this.provider = provider;
		}
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasBody()) {
				configurer = configure(new SequenceConfigurer(provider), config);
			} else if (config.hasValue()) {
				configurer = new FlowReferenceConfigurer().name(config.valueAsString());
			} else {
				invalidConfig("must have body or value");
			}
		}
		
		@Override
		public void setup(OperationSetup ops) {
			configurer.setup(ops);
		}
		
	}
	
	public static class LogConfigurer implements OperationConfigurer {

		private Function<Data,String> msgFn;
		
		@Conf.Val
		public void in(String val) {
			msgFn = StringWithVars.compile(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("log", () -> new LogOperation(msgFn));
		}
		
	}
	
	public static class LogOperation implements Operation {
		
		private final Function<Data,String> msgFn;
		
		public LogOperation(Function<Data,String> msgFn) {
			this.msgFn = msgFn;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			log.info(msgFn.apply(data));
		}
		
	}
	
	public static class CopyConfigurer implements OperationConfigurer {
		
		private final ImmutableList.Builder<Entry<Path,Path>> entries = ImmutableList.builder();

		@Conf.Each
		public void entry(Config item) {
			if (item.hasValue()) {
				entries.add(createEntry(dots(item.key()), dots(item.valueAsString())));
			}
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("copy", () -> new CopyOperation(entries.build()));
		}
		
	}
	
	public static class CopyOperation implements Operation {

		private final List<Entry<Path,Path>> entries;
		
		public CopyOperation(List<Entry<Path,Path>> entries) {
			this.entries = entries;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			for (Entry<Path,Path> e : entries) {
				Data d = data.at(e.getKey());
				
				if (!d.isPresent()) continue;
				
				if (d.isContent()) {
					data.put(e.getValue(), d.content());
				} else {
					Path base = e.getValue();
					d.forEachContent((p, c) -> {
						data.put(base.add(p), c);
					});
				}
				
			}
		}
		
	}
	
	public static class PutConfigurer implements OperationConfigurer {

		private Content content;
		private Data data;
		private Path out = root();
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasSubkey()) {
				out = out.add(dots(config.subkey()));
				if (config.hasDocument()) {
					if (config.hasValue()) {
						out = out.add(dots(config.valueAsString()));
					}
					content = binary(config.documentType(), config.documentContent());
				} else if (config.hasValue()) {
					checkConfig(!config.hasBody(), "can't have a body if you are using subkey and value");
					content = utf8(config.valueAsString());
				} else if (config.hasBody()) {
					if (config.hasValue()) {
						out = out.add(dots(config.valueAsString()));
					}
					data = configToData(config.body());	
				}
			} else if (config.hasDocument()) {
				content = binary(config.documentType(), config.documentContent());
				if (config.hasValue()) {
					out = out.add(dots(config.valueAsString()));
				}
			} else if (config.hasBody()) {
				data = configToData(config.body());
				if (config.hasValue()) {
					out = out.add(dots(config.valueAsString()));
				}
			}
		}
		
		private String name() {
			if (out.isEmpty()) {
				return "put";
			} else {
				return format("put %s", out.dots());
			}
		}

		@Override
		public void setup(OperationSetup ops) {
			if (content != null) {
				ops.meta().put("content", content);
				ops.add(name(), () -> new PutContentOperation(content, out));
			} else if (data != null) {
				ops.meta().put("data", data);
				ops.add(name(), () -> new PutDataOperation(data, out));
			} else {
				invalidConfig("you must have content or data");
			}
		}
		
	}
		
	public static class PutWithVarsConfigurer implements OperationConfigurer {

		private Data data;
		private Path out = root();
		
		@Conf.Config
		public void config(Config config) {
			if (config.hasSubkey()) {
				out = out.add(dots(config.subkey()));
				if (config.hasValue()) {
					checkConfig(!config.hasBody(), "can't have a body if you are using subkey and value");
					data = MutableMemoryData.create().put(root(), utf8(config.valueAsString()));
				} else if (config.hasBody()) {
					if (config.hasValue()) {
						out = out.add(dots(config.valueAsString()));
					}
					data = configToData(config.body());	
				}
			} else if (config.hasBody()) {
				data = configToData(config.body());
				if (config.hasValue()) {
					out = out.add(dots(config.valueAsString()));
				}
			}
		}

		private String name() {
			if (out.isEmpty()) {
				return "putv";
			} else {
				return format("putv %s", out.dots());
			}
		}

		@Override
		public void setup(OperationSetup ops) {
			checkConfig(data != null, "you must have data");
			ops.add(name(), () -> new PutDataWithVarsOperation(data, out));
		}
		
	}
	
	public static class PutContentOperation implements Operation {

		private final Content content;
		private final Path out;
		
		public PutContentOperation(Content content, Path out) {
			this.content = content;
			this.out = out;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			data.put(out, content);
		}
		
	}
	
	public static class PutDataOperation implements Operation {

		private final Data datavalue;
		private final Path out;
		
		public PutDataOperation(Data data, Path out) {
			this.datavalue = data;
			this.out = out;
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			datavalue.forEachContent((path, content) -> {
				data.put(out.add(path), content);
			});
		}
		
	}
	
	public static class PutDataWithVarsOperation implements Operation {
		
		private final Data dataonly;
		private final Map<Path,Function<Data,Content>> vardata;
		
		public PutDataWithVarsOperation(Data data, Path out) {
			
			MutableData fordataonly = MutableMemoryData.create();
			Map<Path,Function<Data,Content>> forvars = new HashMap<>();
			
			data.forEachContent((path, content) -> {
				try {
					String val = content.asUTF8();
					if (StringWithVars.hasVars(val)) {
						forvars.put(out.add(path), StringWithVars.compile(content.asUTF8()).andThen(s -> utf8(s)));
					} else {
						fordataonly.put(out.add(path), content);
					}
				} catch (Throwable t) {
					fordataonly.put(out.add(path), content);
				}
			});
			
			dataonly = fordataonly.immutable();
			vardata = ImmutableMap.copyOf(forvars);
			
		}
		
		@Override
		public void call(MutableData data, OperationContext ctx) {
			dataonly.forEachContent((path, content) -> {
				data.put(path, content);
			});
			vardata.forEach((path, f) -> {
				data.put(path, f.apply(data));
			});
		}
		
	}

}
