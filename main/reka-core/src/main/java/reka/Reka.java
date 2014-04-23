package reka;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static reka.util.Util.runtime;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.admin.RekaSystemBundle;
import reka.config.FileSource;
import reka.config.Source;
import reka.core.bundle.BundleManager;
import reka.core.bundle.RekaBundle;

public class Reka {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final File datadir;
	private final List<RekaBundle> bundles = new ArrayList<>();
	private final List<String> filenames = new ArrayList<>();
	
	public Reka(File datadir, List<RekaBundle> bundles, List<String> filenames) {
		this.datadir = datadir;
		this.bundles.addAll(bundles);
		this.filenames.addAll(filenames);
	}
	
	public void run() {
		
		if (!datadir.isDirectory() && !datadir.mkdirs()) throw runtime("couldn't create datadir %s", datadir); 
		
		BundleManager bundleManager = new BundleManager(bundles);
		ApplicationManager manager  = new ApplicationManager(datadir, bundleManager);
		
		bundleManager.add(new RekaSystemBundle(manager));
		
		Stream<String> bundlesNames = bundleManager.useKeys().stream().map(reka.api.Path::slashes);
		log.info("available bundles {}", bundlesNames.filter(s -> !s.isEmpty()).collect(toList()));
		
		log.info("starting reka");

		manager.restore();
		
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
			
			String identity = UUID.randomUUID().toString();
			
			Source source = FileSource.from(file);
			
			manager.deployTransient(identity, source);
			
		}
	}

	//public static void runWithCommandlineArgumentsAndBundles(String[] args, RekaBundle... bundles) throws CmdLineException {
		
		/*
		CommandlineRunner runner = new CommandlineRunner(bundles);
		CmdLineParser parser = new CmdLineParser(runner);
		parser.parseArgument(args);
		runner.run();
		*/
		
		/*
		if (runner.command != null) {
			runner.command.run();
		} else {
			parser.setUsageWidth(160);
			parser.printUsage(System.err);
			System.exit(0);
		}
		*/

	//}
	
}
