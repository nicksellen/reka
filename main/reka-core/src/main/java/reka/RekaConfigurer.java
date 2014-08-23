package reka;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static reka.util.Util.unchecked;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.RekaBundle;

public class RekaConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final List<String> appPaths = new ArrayList<>();
	private final List<ConfigBody> appConfigs = new ArrayList<>();
	
	private String datadir = "data";

	private final List<RekaBundle> defaultBundles = new ArrayList<>();
	
	private final Map<URL, String> addedBundles = new HashMap<>();
	
	private final Path bundleBasedir;
	
	public RekaConfigurer(Path bundleBasedir, List<RekaBundle> defaultBundles) {
		this.bundleBasedir = bundleBasedir;
		this.defaultBundles.addAll(defaultBundles);
	}
	
	@Conf.At("data")
	public void datadir(String val) {
		datadir = val;
	}
	
	@Conf.Each("bundle")
	public void bundle(String val) {
		unpackBundle(val);
	}
	
	@Conf.Each("run")
	public void app(String val) {
		appPaths.add(val);
	}
	
	@Conf.Each("app")
	public void appDefinition(Config config) {
		if (!config.hasBody()) return;
		ConfigBody body = config.body();
		appConfigs.add(body);
	}
	
	private void unpackBundle(String jarpath) {
		log.info("loading bundles from {}", jarpath);
		loadBundle(bundleBasedir.resolve(jarpath).toFile());
	}
	
	private void loadBundle(File file) {
		try {
			checkArgument(file.exists(), "[%s] does not exist", file.getAbsolutePath());
			try (ZipFile zip = new ZipFile(file)) {
				ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
				checkArgument(entry != null, "must include META-INF/MANIFEST.MF in your jar");
				Manifest manifest = new Manifest(zip.getInputStream(entry));

				String coreVersion = manifest.getMainAttributes().getValue("Reka-Version");
				checkArgument(coreVersion != null, "you must include a Reka-Version value in the manifest, specifiying the core version");
				
				// TODO: actually make it check the version against the self version
				
				String bundleName = manifest.getMainAttributes().getValue("Reka-Bundle");
				checkArgument(bundleName != null, "you must include a Reka-Bundle value in the manifest");
				addedBundles.put(file.toURI().toURL(), bundleName);
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}
	
	public Reka build() {
		
		List<RekaBundle> bundles = new ArrayList<>();
		
		bundles.addAll(defaultBundles);
		
		URL[] urls = addedBundles.keySet().toArray(new URL[addedBundles.size()]);
		
		boolean classLoadingError = false;
		for (Entry<URL, String> e : addedBundles.entrySet()) {
			
			// classloader per bundle keeps things isolated (each bundle can have it's own versions of libraries)
			@SuppressWarnings("resource")
			ClassLoader cl = new URLClassLoader(urls, getClass().getClassLoader());
			
			String classname = e.getValue();
			try {
				Object obj = cl.loadClass(classname).newInstance();
				bundles.add((RekaBundle) obj);
			} catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException error) {
				error.printStackTrace();
				log.error("couldn't load {} from {}", classname, e.getKey());
				classLoadingError = true;
			}
		}
		
		checkState(!classLoadingError, "failed to load all bundles");
		
		return new Reka(new File(datadir), bundles, appPaths, appConfigs);
	}

}
