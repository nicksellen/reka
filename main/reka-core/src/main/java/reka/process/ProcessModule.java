package reka.process;

import static com.google.common.collect.Iterables.toArray;
import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.core.setup.OperationSetup;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationConfigurer;

import com.google.common.base.Splitter;

public class ProcessModule extends ModuleConfigurer {
	
	protected static final IdentityKey<ProcessManager> PROCESS_MANAGER = IdentityKey.named("process manager");
	protected static final IdentityKey<Consumer<String>> TRIGGER_CONSUMER = IdentityKey.named("trigger consumer");

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private String[] command;
	
	private boolean noreply = false;
	
	private ConfigBody onLine;
	
	@Conf.At("command")
	public void command(String val) {
		command = toArray(Splitter.on(" ").split(val), String.class); // TODO make it better, splitting on space is just wrong!
	}
	
	@Conf.At("noreply")
	public void noreply(Config unused) {
		noreply = true;
	}
	
	@Conf.At("script")
	public void script(Config config) {
		checkConfig(command == null, "don't set command and script");
		checkConfig(config.hasDocument(), "must have document");
		try {
			File file = Files.createTempFile("reka", "externalscript").toFile();
			Files.write(file.toPath(), config.documentContent());
			file.deleteOnExit();
			file.setExecutable(true, true);
			command = new String[] { file.getAbsolutePath() };
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	@Conf.Each("on")
	public void on(Config config) {
		checkConfig(config.hasValue(), "must have a value");
		checkConfig(config.hasBody(), "must have a body");
		switch (config.valueAsString()) {
		case "line":
			onLine = config.body();
			break;
		default:
			throw runtime("unknown trigger %s", config.valueAsString());
		}
	}

	@Override
	public void setup(ModuleSetup module) {
		
		if (onLine != null) {
			module.trigger("on line", onLine, registration -> {
				Flow flow = registration.flow();
				registration.store().get(PROCESS_MANAGER).addLineTrigger(line -> {
					flow.prepare().data(MutableMemoryData.create().putString("out", line)).run();
				});
			});
		}
		
		module.setupInitializer(init -> {
			init.run("start process", store -> {
				try {
					ProcessBuilder builder = new ProcessBuilder();
					builder.command(command);
					log.info("starting {}\n", asList(command));
					builder.environment().clear();
					store.put(PROCESS_MANAGER, new MultiProcessManager(builder, Runtime.getRuntime().availableProcessors(), noreply));
				} catch (Exception e) {
					throw unchecked(e);
				}
			});
		});
		
		module.shutdown("kill process", store -> {
			store.lookup(PROCESS_MANAGER).ifPresent(manager -> manager.kill());
		});
		
		module.operation(root(), provider -> new ProcessCallConfigurer(noreply));
	}
	
	public static class ProcessCallConfigurer implements OperationConfigurer {

		private final boolean noreply;
		
		private Function<Data,String> lineFn = (data) -> data.toJson();
		
		public ProcessCallConfigurer(boolean noreply) {
			this.noreply = noreply;
		}
		
		@Conf.Val
		public void line(String val) {
			lineFn = StringWithVars.compile(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			if (noreply) {
				ops.add("call (noreply)", store -> new ProcessCallNoreplyOperation(store.get(PROCESS_MANAGER), lineFn));
			} else {
				ops.add("call", store -> new ProcessCallOperation(store.get(PROCESS_MANAGER), lineFn));
			}
		}
		
	}
	
	public static class ProcessCallNoreplyOperation implements Operation {

		private final ProcessManager manager;
		private final Function<Data,String> lineFn;
		
		public ProcessCallNoreplyOperation(ProcessManager manager, Function<Data,String> lineFn) {
			this.manager = manager;
			this.lineFn = lineFn;
		}
		
		@Override
		public void call(MutableData data) {
			manager.run(lineFn.apply(data));
		}
		
	}
	
	public static class ProcessCallOperation implements AsyncOperation {

		private final ProcessManager manager;
		private final Function<Data,String> lineFn;
		
		public ProcessCallOperation(ProcessManager manager, Function<Data,String> lineFn) {
			this.manager = manager;
			this.lineFn = lineFn;
		}
		
		@Override
		public void call(MutableData data, OperationResult ctx) {
			manager.run(lineFn.apply(data), output -> {
				data.putString("out", output);
				ctx.done();
			});
		}
		
	}

}
