package reka.all;

import static java.util.Arrays.asList;
import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.JsonBundle;
import reka.RekaConfigurer;
import reka.builtins.BuiltinsBundle;
import reka.common.CommonBundle;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.core.bundle.BundleConfigurer;
import reka.core.bundle.BundleManager;
import reka.filesystem.FilesystemBundle;
import reka.h2.H2Bundle;
import reka.http.HttpBundle;
import reka.jade.JadeBundle;
import reka.jsx.JsxBundle;
import reka.less.LessBundle;
import reka.main.Main;
import reka.mustache.MustacheBundle;
import reka.nashorn.NashornBundle;
import reka.postgres.PostgresBundle;
import reka.process.ProcessBundle;

public class All {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
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
		
		List<BundleConfigurer> defaultBundles = new ArrayList<>(asList(
			new BuiltinsBundle(), 
			new FilesystemBundle(),
			new NashornBundle(),
			new ProcessBundle(),
			new MustacheBundle(),
			new H2Bundle(),
			new PostgresBundle(),
			new JsxBundle(),
			new CommonBundle(),
			new LessBundle(),
			new JadeBundle(),
			new JsonBundle()));
		
		defaultBundles.addAll(asList(
			new HttpBundle()));
		
		NavigableConfig conf = new BundleManager(defaultBundles).processor().process(ConfigParser.fromFile(file));
		configure(new RekaConfigurer(file.getParentFile().toPath(), defaultBundles), conf).build().run();
		
	}

}
