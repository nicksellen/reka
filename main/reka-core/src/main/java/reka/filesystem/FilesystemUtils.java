package reka.filesystem;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesystemUtils {
	
	public static Path resolveAndCheck(Path basedir, String other) {
		return check(basedir, basedir.resolve(other).normalize());
	}
	
	public static Path resolveAndCheck(Path basedir, Path other) {
		return check(basedir, basedir.resolve(other).normalize());
		
	}
	
	private static Path check(Path basedir, Path path) {
		try {
			if (!path.startsWith(basedir) || Files.isSameFile(basedir, path)) throw runtime("illegal path");
			return path;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

}
