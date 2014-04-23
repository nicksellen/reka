package reka.main;

import static java.util.Arrays.asList;
import static reka.configurer.Configurer.configure;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.JsonBundle;
import reka.Reka;
import reka.RekaConfigurer;
import reka.builtins.BuiltinsBundle;
import reka.config.parser.ConfigParser;
import reka.core.bundle.RekaBundle;
import reka.filesystem.FilesystemBundle;
import reka.h2.H2Bundle;
import reka.http.HttpBundle;
import reka.jade.JadeBundle;
import reka.mustache.MustacheBundle;
import reka.nashorn.NashornBundle;
import reka.smtp.SmtpBundle;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws CmdLineException {
		
		log.info("starting reka");
		
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
			new HttpBundle(),
			new SmtpBundle(),
			new NashornBundle(),
			new MustacheBundle(),
			new JadeBundle(),
			new H2Bundle(),
			new JsonBundle());
		
		Reka reka = configure(new RekaConfigurer(defaultBundles), ConfigParser.fromFile(file)).build();
		
		reka.run();
		
		/*
		Reka.runWithCommandlineArgumentsAndBundles(args, 
				new BuiltinsBundle(), 
				new FilesystemBundle(),
				new HttpBundle(),
				new SmtpBundle(),
				new MustacheBundle(),
				new JadeBundle(),
				new H2Bundle(),
				new JsonBundle());
				*/
		
	}

}
