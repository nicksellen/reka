package reka.process;

import static java.util.Arrays.asList;
import static reka.api.Path.root;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.builder.FlowSegments.async;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
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
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.util.StringWithVars;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;


public class UseProcess extends UseConfigurer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private String[] command;
	
	@Conf.At("command")
	public void command(String val) {
		command = Iterables.toArray(Splitter.on(" ").split(val), String.class); // TODO make it better, splitting on space is just wrong!
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

	public static class SimpleProcessManager implements ProcessManager {

		private final Logger log = LoggerFactory.getLogger(getClass());
		
		@SuppressWarnings("unused")
		private final ProcessBuilder builder;
		private final Process process;
		
		private final OutputStream stdin;
		private final BufferedReader stdoutReader;
		private final InputStream stdout;
		private final InputStream stderr;
		
		private final Thread thread;
		
		private final BlockingDeque<Entry<String,Consumer<String>>> q;
		
		public SimpleProcessManager(ProcessBuilder builder, BlockingDeque<Entry<String,Consumer<String>>> q) {
			this.q = q;
			this.builder = builder;
			try {
				this.process = builder.start();
			} catch (IOException e1) {
				throw unchecked(e1);
			}
			
			stdin = process.getOutputStream();
			stdout = process.getInputStream();
			stderr = process.getErrorStream();
			
			stdoutReader = new BufferedReader(new InputStreamReader(stdout, Charsets.UTF_8));
			
			thread = new Thread() {
				
				@Override
				public void run() {
					try {
						while (process.isAlive()) {
							Entry<String, Consumer<String>> e = q.take();
							stdoutReader.skip(stdout.available()); // not sure if this is working on not....
							String input = e.getKey();
							byte[] bytes = input.getBytes(Charsets.UTF_8);
							stdin.write(bytes);
							stdin.write(NEW_LINE);
							stdin.flush();	
							String output = stdoutReader.readLine();
							if (output != null) {
								e.getValue().accept(output);
							}
							drain(stderr);
						}	
					} catch (IOException | InterruptedException t) {
						t.printStackTrace();
						log.error("process thread stopped :(");
					}
				}
			};
			
			thread.start();
			
		}
		
		private void drain(InputStream stream) {
			byte[] buf = new byte[8192];
			int readLength;
			try {
				if (stream.available() > 0) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					while ((readLength = stream.read(buf, 0, buf.length)) > 0) { 
						baos.write(buf, 0, readLength);
					}
					System.err.printf("stderr: %s\n", new String(baos.toByteArray(), Charsets.UTF_8));
				}
			} catch (IOException e) {
				e.printStackTrace();
				// ignore
			}
		}
		
		private static final byte[] NEW_LINE = "\n".getBytes(Charsets.UTF_8);
		
		@Override
		public void run(String input, Consumer<String> consumer) {
			if (!process.isAlive()) throw runtime("process is dead!");
			q.offer(createEntry(input, consumer));
		}

		@Override
		public void kill() {
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
		
	}
	
	public static class MultiProcessManager implements ProcessManager {

		private final BlockingDeque<Entry<String,Consumer<String>>> q = new LinkedBlockingDeque<>();
		private final ProcessBuilder builder;
		private final Collection<SimpleProcessManager> all = new ArrayList<>();
		
		public MultiProcessManager(ProcessBuilder builder, int count) {
			this.builder = builder;
			
			for (int i = 0; i < count; i++) {
				all.add(new SimpleProcessManager(this.builder, q));
			}
			
			/*
			this.manager = ThreadLocal.withInitial(() -> {
				SimpleProcessManager m = new SimpleProcessManager(this.builder, q);
				all.add(m);
				return m;
			});
			*/ 
		}
		
		@Override
		public void run(String input, Consumer<String> consumer) {
			//manager.get().run(input, consumer);
			q.offer(createEntry(input, consumer));
		}

		@Override
		public void kill() {
			all.forEach(m -> m.kill());
		}
		
	}
	
	@Override
	public void setup(UseInit use) {
		
		AtomicReference<ProcessManager> managerRef = new AtomicReference<>();
		
		use.run("start process", data -> {
			try {
				ProcessBuilder builder = new ProcessBuilder();
				builder.command(command);
				log.info("starting {}\n", asList(command));
				builder.environment().clear();
				managerRef.set(new MultiProcessManager(builder, Runtime.getRuntime().availableProcessors()));
				return data;
			} catch (Exception e) {
				throw unchecked(e);
			}
		});
		
		use.shutdown("kill process", () -> {
			managerRef.get().kill();
		});
		
		use.operation(root(), () -> new ExternalRunConfigurer(managerRef));
	}
	
	public static class ExternalRunConfigurer implements Supplier<FlowSegment> {

		private final AtomicReference<ProcessManager> managerRef;
		
		private Function<Data,String> lineFn = (data) -> data.toJson();
		
		public ExternalRunConfigurer(AtomicReference<ProcessManager> manager) {
			this.managerRef = manager;
		}
		
		@Conf.Val
		public void line(String val) {
			lineFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return async("call", () -> new ExternalRunOperation(managerRef.get(), lineFn));
		}
		
	}
	
	public static class ExternalRunOperation implements AsyncOperation {

		private final ProcessManager manager;
		private final Function<Data,String> lineFn;
		
		public ExternalRunOperation(ProcessManager manager, Function<Data,String> lineFn) {
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
