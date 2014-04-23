package reka.util;

import static reka.util.Util.unchecked;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Graphviz {
	
	private static final Logger log = LoggerFactory.getLogger(Graphviz.class);
	
	private static final Runtime rt = Runtime.getRuntime();

	public static void writeDotToJPG(String dot, String output) {
		writeDotTo(dot, output, "jpg");
	}
	
	public static void writeDotToSVG(String dot, String output) {
		writeDotTo(dot, output, "svg");
	}
	
	public static void writeDotTo(String dot, String output, String format) {
		String cmd = "/usr/local/bin/dot";
		
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
