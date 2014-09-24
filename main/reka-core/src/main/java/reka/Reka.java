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
import reka.admin.RekaSystemBundle;
import reka.config.ConfigBody;
import reka.core.bundle.BundleConfigurer;
import reka.core.bundle.BundleManager;

public class Reka {
	
	public static final ScheduledExecutorService SCHEDULED_SERVICE = Executors.newSingleThreadScheduledExecutor();
	
	public static final String REKA_ENV = "REKA_ENV";
	
	private static final Logger log = LoggerFactory.getLogger(Reka.class);
	
	private final File datadir;
	private final List<BundleConfigurer> bundles = new ArrayList<>();
	private final Map<String,ConfigBody> configs = new HashMap<>();
	
	public Reka(File datadir, List<BundleConfigurer> bundles, Map<String,ConfigBody> configs) {
		this.datadir = datadir;
		this.bundles.addAll(bundles);
		this.configs.putAll(configs);
	}
	
	public void run() {
		
		if (!datadir.isDirectory() && !datadir.mkdirs()) throw runtime("couldn't create datadir %s", datadir); 
		
		BundleManager bundleManager = new BundleManager(bundles);
		ApplicationManager applicationManager  = new ApplicationManager(datadir, bundleManager);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			
			@Override
			public void run() {
				applicationManager.shutdown();
				bundleManager.shutdown();
			}
			
		});
		
		bundleManager.add(new RekaSystemBundle(applicationManager));
		
		Stream<String> bundlesNames = bundleManager.modulesKeys().stream().map(reka.api.Path::slashes);
		
		bundlesNames.filter(s -> !s.isEmpty()).forEach(m -> {
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
