package reka.util;

import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.app.Application;
import reka.app.ApplicationConfigurer;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.identity.Identity;
import reka.lib.bouncycastle.BouncyCastleLoader;
import reka.module.Module;
import reka.module.ModuleManager;
import reka.module.ModuleMeta;
import reka.modules.builtins.BuiltinsModule;
import reka.util.dirs.BaseDirs;

public class RekaTest {
	
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(RekaTest.class);
	
	public static Application runApp(Module module, File file) throws IOException, InterruptedException, ExecutionException {
		ClassLoader classLoader = BouncyCastleLoader.createClassLoader(Reka.class.getClassLoader());
		List<ModuleMeta> modules = new ArrayList<>(); 
		modules.add(new ModuleMeta(classLoader, "core", new BuiltinsModule()));
		if (module != null) {
			modules.add(new ModuleMeta(classLoader, "core", module));
		}
		ModuleManager moduleManager = new ModuleManager(modules);
		NavigableConfig config = moduleManager.processor().process(ConfigParser.fromFile(file));
		Path dir = Files.createTempDirectory("reka-test");
		dir.toFile().deleteOnExit();
		BaseDirs dirs = new BaseDirs(dir.resolve("app"), dir.resolve("data"), dir.resolve("tmp"));
		ApplicationConfigurer appConfigurer = configure(new ApplicationConfigurer(dirs.mktemp(), moduleManager), config);
		return appConfigurer.build(Identity.create(file.getPath()), 0, Collections.emptyMap()).get();
	}
	
	public static TestSuite createTestSuiteFrom(Module module, File... files) {
		TestSuite suite = new TestSuite("reka tests");
		for (File file : files) {
			suite.addTest(new RekaTestCase(module, file));
		}
		return suite;
	}
	
	public static class RekaTestCase extends TestCase {
		
		private final Module module;
		private final File file;
		
		public RekaTestCase(Module module, File file) {
			super(file.getPath());
			this.module = module;
			this.file = file;
		}
		
		@Override
		public void runTest() throws Throwable {
			RekaTest.runApp(module, file);
		}
		
	}
	
}
