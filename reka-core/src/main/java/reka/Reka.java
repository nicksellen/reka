package reka;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.app.manager.ApplicationManager;
import reka.app.manager.ApplicationManager.DeploySubscriber;
import reka.config.ConfigBody;
import reka.config.FileSource;
import reka.module.ModuleManager;
import reka.module.ModuleMeta;
import reka.modules.admin.AdminModule;
import reka.util.AsyncShutdown;
import reka.util.DaemonThreadFactory;
import reka.util.Path;
import reka.util.dirs.AppDirs;
import reka.util.dirs.BaseDirs;

public class Reka {
	
	public static interface SharedExecutors {
		public static final ExecutorService general = Executors.newCachedThreadPool(new DaemonThreadFactory());
		public static final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
	}
	
	private static final Logger log = LoggerFactory.getLogger(Reka.class);
	
	private final BaseDirs dirs;
	private final List<ModuleMeta> modules = new ArrayList<>();
	private final Map<Path,ConfigBody> configs = new HashMap<>();
	
	public Reka(BaseDirs dirs, List<ModuleMeta> modules, Map<Path,ConfigBody> configs) {
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
				long started = System.nanoTime();
				System.out.printf("shutdown started\n");
				try {
					AsyncShutdown.shutdownAll(manager, moduleManager).get(10, TimeUnit.SECONDS);
					long took = (System.nanoTime() - started) / 1000000;
					System.out.printf("shutdown complete after %dms\n", took);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					long took = (System.nanoTime() - started) / 1000000;
					System.err.printf("shutdown complete with error after %dms\n", took);
				}
				System.out.flush();
				System.err.flush();
			}
			
		});
		
		moduleManager.add(new ModuleMeta(Reka.class.getClassLoader(), "core", new AdminModule(manager)));
		
		Stream<String> moduleNames = moduleManager.modulesKeys().stream().map(reka.util.Path::slashes);
		
		moduleNames.filter(s -> !s.isEmpty()).forEach(m -> {
			log.info("module available {}", m);
		});
		
		log.info("starting with apps dirs {}", dirs.app().toString());

		for (Entry<Path, ConfigBody> e : configs.entrySet()) {
			manager.deployConfig(e.getKey(), -1, e.getValue(), null, DeploySubscriber.LOG);
		}
		
		AppDirs.listApps(dirs).forEach((pathAndVersion, path) -> {
			File mainreka = path.resolve("main.reka").toFile();
			if (!mainreka.exists()) return;
			manager.deploySource(pathAndVersion.path(), pathAndVersion.version(), FileSource.from(mainreka), DeploySubscriber.LOG);
		});
	}
	
}
