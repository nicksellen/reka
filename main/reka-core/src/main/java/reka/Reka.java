package reka;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.runtime;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.admin.RekaSystemBundle;
import reka.config.ConfigBody;
import reka.config.FileSource;
import reka.config.Source;
import reka.core.bundle.BundleManager;
import reka.core.bundle.RekaBundle;

public class Reka {
	
	public static final String REKA_ENV = "REKA_ENV";
	
	private static final Logger log = LoggerFactory.getLogger(Reka.class);
	
	private final File datadir;
	private final List<RekaBundle> bundles = new ArrayList<>();
	private final List<String> filenames = new ArrayList<>();
	private final Map<String,ConfigBody> configs = new HashMap<>();
	
	public Reka(File datadir, List<RekaBundle> bundles, List<String> filenames, Map<String,ConfigBody> configs) {
		this.datadir = datadir;
		this.bundles.addAll(bundles);
		this.filenames.addAll(filenames);
		this.configs.putAll(configs);
	}
	
	public void run() {
		
		if (!datadir.isDirectory() && !datadir.mkdirs()) throw runtime("couldn't create datadir %s", datadir); 
		
		BundleManager bundleManager = new BundleManager(bundles);
		ApplicationManager applicationManager  = new ApplicationManager(datadir, bundleManager, false);
		
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

		applicationManager.restore();
		
		for (String filename : filenames) {
			File possibleFile = new File(filename);
			
			if (!possibleFile.exists()) {
				URL resource = getClass().getResource('/' + filename);
				if (resource != null) {
					possibleFile = new File(resource.getFile());
				}
			}

			checkArgument(possibleFile.exists(), "can't find [%s]", filename);
			
			final File file = possibleFile;
			String identity = file.toPath().getFileName().toString().replaceFirst("\\.reka$", "");
			Source source = FileSource.from(file);
			applicationManager.deployTransient(identity, source);
		}
		
		for (Entry<String, ConfigBody> e : configs.entrySet()) {
			applicationManager.deployConfig(e.getKey(), e.getValue());
		}
		
	}
	
}
