package reka.filesystem;

import static reka.util.Util.runtime;

import java.nio.file.Path;

public class FilesystemUtils {
	
	public static Path resolveAndCheck(Path basedir, String other) {
		return check(basedir, basedir.resolve(other).normalize());
	}
	
	public static Path resolveAndCheck(Path basedir, Path other) {
		return check(basedir, basedir.resolve(other).normalize());
		
	}
	
	private static Path check(Path basedir, Path path) {
		if (!path.startsWith(basedir)) throw runtime("illegal path");
		return path;
	}

}
