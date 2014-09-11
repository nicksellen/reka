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

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.EmbeddedFlowConfigurer;
import reka.api.Path;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.Configurer.ErrorCollector;
import reka.config.configurer.ErrorReporter;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BuiltinsModule extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(BuiltinsModule.class);

	@Override
	public void setup(ModuleSetup module) {
		
		module.operation(path("put"), provider -> new PutConfigurer());
		module.operation(path("putv"), provider -> new PutWithVarsConfigurer());
		module.operation(path("copy"), provider -> new CopyConfigurer());
    	module.operation(path("run"), provider -> new RunConfigurer(provider));
    	module.operation(path("runp"), provider -> new RunParallelConfigurer(provider));
    	module.operation(path("then"), provider -> new RunConfigurer(provider));
    	module.operation(path("log"), provider -> new LogConfigurer());
    	module.operation(path("stringwithvariables"), provider -> new StringWithVariablesConfigurer());
    	module.operation(path("sleep"), provider -> new SleepConfigurer());
    	module.operation(path("halt!"), provider -> new HaltConfigurer());
    	module.operation(slashes("uuid/generate"), provider -> new GenerateUUIDConfigurer());
    	module.operation(path("println"), provider -> new PrintlnConfigurer());
    	
    	module.operation(slashes("bcrypt/hashpw"), provider -> new BCryptHashpwConfigurer());
    	module.operation(slashes("bcrypt/checkpw"), provider -> new BCryptCheckpwConfigurer(provider));
    	
    	module.operation(path("throw"), provider -> new ThrowConfigurer());
    	module.operation(path("inspect"), provider -> new InspectConfigurer());
    	module.operation(slashes("random/string"), provider -> new RandomStringConfigurer());
    	module.operation(path("coerce"), provider -> new Coercion.CoerceConfigurer());
    	module.operation(slashes("coerce/int64"), provider -> new Coercion.CoerceLongConfigurer());
    	module.operation(slashes("coerce/bool"), provider -> new Coercion.CoerceBooleanConfigurer());
    	module.operation(path("unzip"), provider -> new UnzipConfigurer());
    	module.operation(path("split"), provider -> new SplitConfigurer());
		
	}
	
	public static class SplitConfigurer implements OperationConfigurer, ErrorReporter {
		
		private Function<Data,Path> inFn, outFn;
		private String on = ",";
		
		@Conf.Val
		@Conf.At("in")
		public void in(String val) {
			inFn = StringWithVars.compile(val).andThen(s -> dots(s));
			if (outFn == null) outFn = inFn;
		}

		@Conf.At("out")
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
			ops.add("split", store -> new SplitOperation(inFn, outFn, Splitter.on(on)));
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
		public void call(MutableData data) {
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
	
	public static class BCryptCheckpwConfigurer implements OperationConfigurer {

		private final ConfigurerProvider provider;
		
		private Path readPwFrom = dots("bcrypt.pw");
		private Path readHashFrom = dots("bcrypt.hash");
		
		private OperationConfigurer ok;
		private OperationConfigurer fail;
		
		public BCryptCheckpwConfigurer(ConfigurerProvider provider) {
			this.provider = provider;
		}

		@Conf.At("read-pw-from")
		public void readPwFrom(String val) {
			readPwFrom = dots(val);
		}
		
		@Conf.At("read-hash-from")
		public void readHashFrom(String val) {
			readHashFrom = dots(val);
		}
		
		@Conf.At("ok")
		public void ok(Config config) {
			ok = configure(new SequenceConfigurer(provider), config);
		}
		
		@Conf.At("fail")
		public void fail(Config config) {
			fail = configure(new SequenceConfigurer(provider), config);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.router("bcrypt/checkpw", store -> new BCryptCheckpwOperation(readPwFrom, readHashFrom), router -> {
				router.add("ok", ok);
				router.add("fail", fail);
			});
		}
		
	}
	
	public static class BCryptCheckpwOperation implements RoutingOperation {

		private final Path readPwFrom;
		private final Path readHashFrom;

		public BCryptCheckpwOperation(Path readPwFrom, Path readHashFrom) {
			this.readPwFrom = readPwFrom;
			this.readHashFrom = readHashFrom;
		}
		
		@Override
		public void call(MutableData data, RouteCollector router) {
			router.defaultRoute("fail");
			data.getContent(readPwFrom).ifPresent(pw -> {
				data.getContent(readHashFrom).ifPresent(hash -> {
					router.routeTo("ok");
				});
			});
		}
		
	}
	
	public static class BCryptHashpwConfigurer implements OperationConfigurer {

		private Path in = dots("bcrypt.pw");
		private Path out = dots("bcrypt.hash");
		
		@Conf.At("in")
		public void in(String val) {
			in = dots(val);
		}
		
		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("bcrypt/hashpw", store -> new BCryptHashpwOperation(in, out));
		}
		
	}
	
	public static class BCryptHashpwOperation implements Operation {

		private final Path in;
		private final Path out;

		public BCryptHashpwOperation(Path in, Path out) {
			this.in = in;
			this.out = out;
		}
		
		@Override
		public void call(MutableData data) {
			data.getContent(in).ifPresent(content -> {
				data.putString(out, BCrypt.hashpw(content.asUTF8(), BCrypt.gensalt()));
			});
		}
		
	}
	
	private static final Random RANDOM = new Random();
	
	public static class InspectConfigurer implements OperationConfigurer {

		@Override
		public void setup(OperationSetup ops) {
			ops.add("inspect", store -> new InspectOperation());
		}
		
	}
	
	public static class InspectOperation implements Operation {

		@Override
		public void call(MutableData data) {
			data.forEachContent((path, content) -> {
				log.debug("{} ->|{}|<- ({})", path.dots(), content, content.getClass());
			});
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
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("random/string", store -> new RandomStringOperation(length, chars, RANDOM, out));
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
		public void call(MutableData data) {
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
			ops.add("throw", store -> new ThrowOperation(msgFn));
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
			ops.add("println", store -> new PrintlnOperation(msg));
		}
		
	}
	
	public static class PrintlnOperation implements Operation {

		private final Function<Data,String> msg;
		
		public PrintlnOperation(Function<Data,String> msg) {
			this.msg = msg;
		}
		
		@Override
		public void call(MutableData data) {
			System.out.println(msg.apply(data));
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
			ops.add("uuid/generate", store -> new GenerateUUIDOperation(out));
		}
		
	}
	
	public static class GenerateUUIDOperation implements Operation {

		private final Path out;
		
		public GenerateUUIDOperation(Path out) {
			this.out = out;
		}
		
		@Override
		public void call(MutableData data) {
			data.putString(out, UUID.randomUUID().toString());
		}
		
	}
	
	public static class SleepConfigurer implements OperationConfigurer {

		private long ms = 1000L;
		
		@Conf.Val
		public void timeout(String val) {
			ms = Long.valueOf(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("sleep", store -> new SleepOperation(ms));
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
		public void call(MutableData data, OperationResult ctx) {
			e.schedule(() -> ctx.done(), ms, TimeUnit.MILLISECONDS);
		}
	}
	
	public static class RunParallelConfigurer implements OperationConfigurer {
		
		private final ConfigurerProvider provider;
		
		private final List<OperationConfigurer> items = new ArrayList<>();
		
		public RunParallelConfigurer(ConfigurerProvider provider) {
			this.provider = provider;
		}
		
		@Conf.Each
		public void item(Config config) {
			log.debug("configuring parallel: {}", config.key());
			items.add(configure(new SequenceConfigurer(provider), ConfigBody.of(config.source(), config)));
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.parallel(par -> {
				items.forEach(item -> par.add(item));
			});
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
			ops.add("stringwithvariables", store -> new DataContentFunctionOperation(template, out));
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
		public void call(MutableData data) {
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
				configurer = new EmbeddedFlowConfigurer().name(config.valueAsString());
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
			ops.add("log", store -> new LogOperation(msgFn));
		}
		
	}
	
	public static class LogOperation implements Operation {
		
		private final Function<Data,String> msgFn;
		
		public LogOperation(Function<Data,String> msgFn) {
			this.msgFn = msgFn;
		}

		@Override
		public void call(MutableData data) {
			log.info(msgFn.apply(data));
		}
		
	}
	
	public static class CopyConfigurer implements OperationConfigurer {
		
		private final ImmutableList.Builder<Entry<Path,Path>> entries = ImmutableList.builder();

		@Conf.Each
		public void entry(Config item) {
			if (item.hasValue()) {
				log.debug("adding copy [{}] -> [{}]", dots(item.key()).dots(), dots(item.valueAsString()).dots());
				entries.add(createEntry(dots(item.key()), dots(item.valueAsString())));
			}
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.add("copy", store -> new CopyOperation(entries.build()));
		}
		
	}
	
	public static class CopyOperation implements Operation {

		private final List<Entry<Path,Path>> entries;
		
		public CopyOperation(List<Entry<Path,Path>> entries) {
			this.entries = entries;
		}
		
		@Override
		public void call(MutableData data) {
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
				ops.add(name(), store -> new PutContentOperation(content, out));
			} else if (data != null) {
				ops.meta().put("data", data);
				ops.add(name(), store -> new PutDataOperation(data, out));
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
			ops.add(name(), store -> new PutDataWithVarsOperation(data, out));
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
		public void call(MutableData data) {
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
		public void call(MutableData data) {
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
		public void call(MutableData data) {
			dataonly.forEachContent((path, content) -> {
				data.put(path, content);
			});
			vardata.forEach((path, f) -> {
				data.put(path, f.apply(data));
			});
		}
		
	}

}
