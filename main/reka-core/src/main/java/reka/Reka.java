package reka;

import static reka.util.Util.runtime;

import java.io.File;
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
import reka.core.module.ModuleManager;

public class Reka {
	
	public static final ScheduledExecutorService SCHEDULED_SERVICE = Executors.newSingleThreadScheduledExecutor();
	
	public static final String REKA_ENV = "REKA_ENV";
	
	private static final Logger log = LoggerFactory.getLogger(Reka.class);
	
	private final File datadir;
	private final List<ModuleMeta> modules = new ArrayList<>();
	private final Map<String,ConfigBody> configs = new HashMap<>();
	
	public Reka(File datadir, List<ModuleMeta> modules, Map<String,ConfigBody> configs) {
		this.datadir = datadir;
		this.modules.addAll(modules);
		this.configs.putAll(configs);
	}
	
	public void run() {
		
		if (!datadir.isDirectory() && !datadir.mkdirs()) throw runtime("couldn't create datadir %s", datadir); 
		
		ModuleManager moduleManager = new ModuleManager(modules);
		ApplicationManager applicationManager  = new ApplicationManager(datadir, moduleManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			
			@Override
			public void run() {
				applicationManager.shutdown();
				moduleManager.shutdown();
			}
			
		});
		
		moduleManager.add(new ModuleMeta("core", new AdminModule(applicationManager)));
		
		Stream<String> moduleNames = moduleManager.modulesKeys().stream().map(reka.api.Path::slashes);
		
		moduleNames.filter(s -> !s.isEmpty()).forEach(m -> {
			log.info("module available {}", m);
		});
		
		if (!System.getenv().containsKey(REKA_ENV)) {
			throw runtime("please ensure %s has been set in your environment", REKA_ENV);
		}
		
		log.info("starting reka in {}", System.getenv(REKA_ENV));
		
		for (Entry<String, ConfigBody> e : configs.entrySet()) {
			applicationManager.deployConfig(e.getKey(), e.getValue(), null, DeploySubscriber.LOG);
		}
		
	}
	
}
