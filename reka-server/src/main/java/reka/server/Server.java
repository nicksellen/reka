package reka.server;

import static java.util.Arrays.asList;
import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.BouncyCastleLoader;
import reka.ModuleMeta;
import reka.Reka;
import reka.RekaConfigurer;
import reka.builtins.BuiltinsModule;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.core.module.ModuleManager;

public class Server {

	private static final Logger log = LoggerFactory.getLogger(Server.class);
	
	public static void main(String[] args) throws CmdLineException {
		
		if (args.length != 1) {
			log.error("please provide a configuration file as the first argument");
			return;
		}
		
		File file = new File(args[0]).getAbsoluteFile();
		
		if (!file.exists()) {
			log.error("configuration file {} is missing", args[0]);
			return;
		};
		
		ClassLoader classLoader = BouncyCastleLoader.classLoader(Reka.class.getClassLoader());
		List<ModuleMeta> defaultModules = asList(new ModuleMeta(classLoader, "core", new BuiltinsModule()));
		NavigableConfig conf = new ModuleManager(defaultModules).processor().process(ConfigParser.fromFile(file));
		configure(new RekaConfigurer(file.getParentFile().toPath(), defaultModules, classLoader), conf).build().run();
		
	}

}
