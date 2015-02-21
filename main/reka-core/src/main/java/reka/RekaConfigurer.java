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
import java.util.Optional;
import java.util.UUID;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.module.Module;
import reka.dirs.BaseDirs;

public class RekaConfigurer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Map<String,ConfigBody> apps = new HashMap<>();
	
	private String appdir = "rekadata/apps";
	private String datadir = "rekadata/data";
	private String tmpdir = "rekadata/tmp";

	private final List<ModuleMeta> defaultModules = new ArrayList<>();
	
	private final List<JarModule> addedModules = new ArrayList<>();
	
	private final Path moduleBasedir;
	
	private static class JarModule {
		
		private final URL url;
		private final String classname;
		@SuppressWarnings("unused")
		private final String name;
		private final String version;
		
		public JarModule(URL url, String classname, String name, String version) {
			this.url = url;
			this.classname = classname;
			this.name = name;
			this.version = version;
		}
	}
	
	public RekaConfigurer(Path moduleBasedir, List<ModuleMeta> modules) {
		this.moduleBasedir = moduleBasedir;
		this.defaultModules.addAll(modules);
	}
	
	@Conf.At("appdir")
	public void appdir(String val) {
		appdir = val;
	}
	
	@Conf.At("datadir")
	public void datadir(String val) {
		datadir = val;
	}
	
	@Conf.At("tmpdir")
	public void tmpdir(String val) {
		tmpdir = val;
	}
	
	@Conf.Each("module")
	public void module(String val) {
		unpackModule(val);
	}
	
	@Conf.Each("app")
	public void app(Config config) {
		if (!config.hasBody()) return;
		ConfigBody body = config.body();
		String identity = null;
		if (config.hasValue()) {
			identity = config.valueAsString();
		} else {
			Optional<Config> name = config.body().at("name");
			if (name.isPresent() && name.get().hasValue()) {
				identity = name.get().valueAsString();
			}
		}
		if (identity == null) identity = UUID.randomUUID().toString();
		apps.put(identity, body);
	}
	
	private void unpackModule(String jarpath) {
		log.info("loading modules from {}", jarpath);
		loadModule(moduleBasedir.resolve(jarpath).toFile());
	}
	
	private void loadModule(File file) {
		try {
			checkArgument(file.exists(), "[%s] does not exist", file.getAbsolutePath());
			try (ZipFile zip = new ZipFile(file)) {
				ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
				checkArgument(entry != null, "must include META-INF/MANIFEST.MF in your jar");
				Manifest manifest = new Manifest(zip.getInputStream(entry));

				String name = manifest.getMainAttributes().getValue("Implementation-Title");
				String version = manifest.getMainAttributes().getValue("Implementation-Version");
				String classname = manifest.getMainAttributes().getValue("Reka-Module");
				
				checkArgument(name != null && version != null, 
						"missing Implementation-Title and/or Implementation-Version " +
						"check you have addDefaultImplementationEntries enabled in your maven-jar-plugin configuration");

				checkArgument(classname != null, "must provide Reka-Module in the manifest");
				
				name = name.replaceFirst("^reka\\-", "");
				
				addedModules.add(new JarModule(file.toURI().toURL(), classname, name, version));
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}
	
	public Reka build() {
		
		List<ModuleMeta> modules = new ArrayList<>();
		
		modules.addAll(defaultModules);
		
		boolean classLoadingError = false;
		ClassLoader base = BouncyCastleLoader.classloader(Reka.class.getClassLoader());
		for (JarModule jar : addedModules) {
			ClassLoader cl = new URLClassLoader(new URL[] { jar.url }, base);
			try {
				Object obj = cl.loadClass(jar.classname).newInstance();
				Module module = (Module) obj;
				modules.add(new ModuleMeta(jar.version, module));
				
			} catch (Throwable error) {
				error.printStackTrace();
				log.error("couldn't load {} from {}", jar.classname, jar.url);
				classLoadingError = true;
			}
		}
		
		checkState(!classLoadingError, "failed to load all modules");
		
		return new Reka(new BaseDirs(appdir, datadir, tmpdir), modules, apps);
	}

}
