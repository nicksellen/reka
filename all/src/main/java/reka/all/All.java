package reka.all;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static reka.config.configurer.Configurer.configure;

import java.io.File;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.JsonModule;
import reka.ModuleMeta;
import reka.RekaConfigurer;
import reka.builtins.BuiltinsModule;
import reka.common.CommonModule;
import reka.config.NavigableConfig;
import reka.config.parser.ConfigParser;
import reka.core.module.ModuleManager;
import reka.core.module.RekaGuiceModule;
import reka.exec.ExecModule;
import reka.filesystem.FilesystemModule;
import reka.h2.H2Module;
import reka.irc.IrcModule;
import reka.jade.JadeModule;
import reka.jsx.JsxModule;
import reka.less.LessModule;
import reka.main.Main;
import reka.mustache.MustacheModule;
import reka.nashorn.NashornModule;
import reka.net.NetModule;
import reka.postgres.PostgresModule;
import reka.process.ProcessModule;
import reka.smtp.SmtpModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

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
		
		final Injector injector = Guice.createInjector(new RekaGuiceModule());
		
		List<ModuleMeta> defaultModules = asList(
				BuiltinsModule.class,
				FilesystemModule.class,
				NashornModule.class,
				ProcessModule.class,
				MustacheModule.class,
				H2Module.class,
				ExecModule.class,
				PostgresModule.class,
				JsxModule.class,
				CommonModule.class,
				LessModule.class,
				IrcModule.class,
				JadeModule.class,
				//UseNetPlayModule.class,
				SmtpModule.class,
				JsonModule.class,
				NetModule.class
			).stream().map(c -> new ModuleMeta("snapshot", injector.getInstance(c))).collect(toList());
		
		NavigableConfig conf = new ModuleManager(defaultModules).processor().process(ConfigParser.fromFile(file));
		configure(new RekaConfigurer(file.getParentFile().toPath(), defaultModules), conf).build().run();
		
	}

}
