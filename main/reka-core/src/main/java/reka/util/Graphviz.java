package reka.util;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static reka.util.Util.unchecked;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Graphviz {
	
	private static final Logger log = LoggerFactory.getLogger(Graphviz.class);
	
	private static final Runtime rt = Runtime.getRuntime();
	
	private static final String[] locations = new String[]{ "/bin/dot", "/usr/bin/dot", "/usr/local/bin/dot" };
	
	private static volatile String cmd;
	
	static {
		for (String location : locations) {
			Path path = Paths.get(location);
			if (Files.exists(path)) {
				cmd = location;
				break;
			}
		}
		if (cmd == null) {
			log.warn("could not find 'dot' program, looked in {}", asList(locations));
		}
	}

	public static void writeDotToJPG(String dot, String output) {
		writeDotTo(dot, output, "jpg");
	}
	
	public static void writeDotToSVG(String dot, String output) {
		writeDotTo(dot, output, "svg");
	}
	
	public static void writeDotTo(String dot, String output, String format) {
		checkState(cmd != null, "'dot' program is not available");
		try {
			File f = File.createTempFile("dot", "dot");
			Files.write(f.toPath(), dot.getBytes());
			try {
				String[] args = new String[]{cmd, "-T", format, "-o", output, f.getAbsolutePath()};
				log.debug("wrote graphviz image to [{}] exit={}\n", output, rt.exec(args).waitFor());
			} finally {
				f.delete();
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}

}
