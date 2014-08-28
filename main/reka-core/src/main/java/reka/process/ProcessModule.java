package reka.process;

import static com.google.common.collect.Iterables.toArray;
import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.async;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.SyncOperation;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;
import reka.core.data.memory.MutableMemoryData;
import reka.core.util.StringWithVars;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class ProcessModule extends ModuleConfigurer {

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
		
		AtomicReference<ProcessManager> managerRef = new AtomicReference<>();
		AtomicReference<Consumer<String>> triggerRef = new AtomicReference<>();
		
		if (onLine != null) {
			module.trigger("line", onLine, registration -> {
				triggerRef.set(line -> {
					registration.flow()
						.prepare()
						.data(MutableMemoryData.create().putString("out", line))
						.run();
				});
			});
		}
		
		module.init("start process", data -> {
			try {
				ProcessBuilder builder = new ProcessBuilder();
				builder.command(command);
				log.info("starting {}\n", asList(command));
				builder.environment().clear();
				managerRef.set(new MultiProcessManager(builder, Runtime.getRuntime().availableProcessors(), noreply, triggerRef));
				return data;
			} catch (Exception e) {
				throw unchecked(e);
			}
		});
		
		module.shutdown("kill process", () -> {
			managerRef.get().kill();
		});
		
		module.operation(root(), () -> new ProcessCallConfigurer(managerRef, noreply));
	}
	
	public static class ProcessCallConfigurer implements Supplier<FlowSegment> {

		private final AtomicReference<ProcessManager> managerRef;
		private final boolean noreply;
		
		private Function<Data,String> lineFn = (data) -> data.toJson();
		
		public ProcessCallConfigurer(AtomicReference<ProcessManager> manager, boolean noreply) {
			this.managerRef = manager;
			this.noreply = noreply;
		}
		
		@Conf.Val
		public void line(String val) {
			lineFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			if (noreply) {
				return sync("call (noreply)", () -> new ProcessCallNoreplyOperation(managerRef.get(), lineFn));
			} else {
				return async("call", () -> new ProcessCallOperation(managerRef.get(), lineFn));
			}
		}
		
	}
	
	public static class ProcessCallNoreplyOperation implements SyncOperation {

		private final ProcessManager manager;
		private final Function<Data,String> lineFn;
		
		public ProcessCallNoreplyOperation(ProcessManager manager, Function<Data,String> lineFn) {
			this.manager = manager;
			this.lineFn = lineFn;
		}
		
		@Override
		public MutableData call(MutableData data) {
			manager.run(lineFn.apply(data));
			return data;
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
		public ListenableFuture<MutableData> call(MutableData data) {
			SettableFuture<MutableData> f = SettableFuture.create();
			manager.run(lineFn.apply(data), output -> {
				data.putString("out", output);
				f.set(data);
			});
			return f;
		}
		
	}

}
