package reka;

import static reka.util.Util.decode64;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager.DeploySubscriber;
import reka.admin.AdminModule;
import reka.config.ConfigBody;
import reka.config.FileSource;
import reka.core.module.ModuleManager;

public class Reka {
	
	public static final ScheduledExecutorService SCHEDULED_SERVICE = Executors.newSingleThreadScheduledExecutor();
	
	public static final String REKA_ENV = "REKA_ENV";
	
	private static final Logger log = LoggerFactory.getLogger(Reka.class);
	
	private final BaseDirs dirs;
	private final List<ModuleMeta> modules = new ArrayList<>();
	private final Map<String,ConfigBody> configs = new HashMap<>();
	
	public Reka(BaseDirs dirs, List<ModuleMeta> modules, Map<String,ConfigBody> configs) {
		this.dirs = dirs;
		this.modules.addAll(modules);
		this.configs.putAll(configs);
	}
	
	public void run() {
		
		dirs.mkdirs();
		dirs.tmp().toFile().deleteOnExit();
		
		ModuleManager moduleManager = new ModuleManager(modules);
		ApplicationManager manager  = new ApplicationManager(dirs, moduleManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			
			@Override
			public void run() {
				manager.shutdown();
				moduleManager.shutdown();
			}
			
		});
		
		moduleManager.add(new ModuleMeta("core", new AdminModule(manager)));
		
		Stream<String> moduleNames = moduleManager.modulesKeys().stream().map(reka.api.Path::slashes);
		
		moduleNames.filter(s -> !s.isEmpty()).forEach(m -> {
			log.info("module available {}", m);
		});
		
		if (!System.getenv().containsKey(REKA_ENV)) {
			throw runtime("please ensure %s has been set in your environment", REKA_ENV);
		}
		
		log.info("starting reka in {}", System.getenv(REKA_ENV));

		for (Entry<String, ConfigBody> e : configs.entrySet()) {
			manager.deployConfig(e.getKey(), e.getValue(), null, DeploySubscriber.LOG);
		}
		
		try {
			Files.list(dirs.app()).forEach(path -> {
				String identity = decode64(path.getFileName().toString());
				File mainreka = path.resolve("main.reka").toFile();
				if (!mainreka.exists()) return;
				manager.deploySource(identity, FileSource.from(mainreka), DeploySubscriber.LOG);
			});
		} catch (IOException e1) {
			throw unchecked(e1);
		}
		
	}
	
}
