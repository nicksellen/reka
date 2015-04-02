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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.AppSetup;
import reka.core.setup.ModuleSetupContext;

import com.google.common.base.Splitter;

public class ProcessConfigurer extends ModuleConfigurer {
	
	protected static final IdentityKey<ProcessManager> PROCESS_MANAGER = IdentityKey.named("process manager");
	protected static final IdentityKey<Consumer<String>> TRIGGER_CONSUMER = IdentityKey.named("trigger consumer");

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private String[] command;
	
	private int processes = Runtime.getRuntime().availableProcessors();
	
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
	
	@Conf.At("processes")
	public void processes(int val) {
		checkConfig(val > 0, "must specifcy at least one");
		processes = val;
	}
	
	@Conf.At("script")
	public void script(Config config) {
		checkConfig(command == null, "don't set command and script");
		checkConfig(config.hasDocument(), "must have document");
		try {
			File file = Files.createTempFile(dirs().tmp(), "script.", "").toFile();
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
	public void setup(AppSetup app) {
		
		ModuleSetupContext ctx = app.ctx();
		
		if (onLine != null) {
			app.buildFlow("on line", onLine, flow -> {
				
				ProcessManager manager = ctx.get(PROCESS_MANAGER);
				
				manager.addListener(line -> {
					flow.prepare().mutableData(MutableMemoryData.create().putString("out", line)).run();
				});
				
			});
		}
		
		app.onDeploy(init -> {
			init.run("start process", () -> {
				try {
					ProcessBuilder builder = new ProcessBuilder();
					builder.command(command);
					log.info("starting {}\n", asList(command));
					builder.environment().clear();
					ProcessManager manager;
					if (processes == 1) {
						manager = new SingleProcessManager(builder, noreply);
					} else {
						manager = new MultiProcessManager(builder, processes, noreply);
					}
					manager.start();
					ctx.put(PROCESS_MANAGER, manager);
				} catch (Exception e) {
					throw unchecked(e);
				}
			});
		});
		
		app.registerStatusProvider(() -> ctx.get(PROCESS_MANAGER));
		
		app.onUndeploy("kill process", () -> {
			ctx.lookup(PROCESS_MANAGER).ifPresent(ProcessManager::shutdown);
		});
		
		app.defineOperation(root(), provider -> new ProcessCallConfigurer(noreply));
	}

}
