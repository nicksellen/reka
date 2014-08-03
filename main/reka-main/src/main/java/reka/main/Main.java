package reka.main;

import static java.util.Arrays.asList;
import static reka.configurer.Configurer.configure;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.JsonBundle;
import reka.RekaConfigurer;
import reka.builtins.BuiltinsBundle;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.core.bundle.BundleManager;
import reka.core.bundle.RekaBundle;
import reka.filesystem.FilesystemBundle;
import reka.nashorn.NashornBundle;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws CmdLineException {
		
		if (args.length != 1) {
			log.error("please provide a configuration file as the first argument");
			return;
		}
		
		File file = new File(args[0]);
		
		if (!file.exists()) {
		log.error("configuration file {} is missing", args[0]);
			return;
		};
		
		List<RekaBundle> defaultBundles = asList(
			new BuiltinsBundle(), 
			new FilesystemBundle(),
			new NashornBundle(),
			new JsonBundle());
		
		NavigableConfig conf = new BundleManager(defaultBundles).processor().process(ConfigParser.fromFile(file));
		configure(new RekaConfigurer(file.getParentFile().toPath(), defaultBundles), conf).build().run();
		
	}

}
