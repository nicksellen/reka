package reka.modules.filesystem;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesystemUtils {
	
	public static Path resolveAndCheck(Path basedir, String other) {
		if (other.startsWith("/")) other = other.substring(1);
		return check(basedir, basedir.resolve(other).normalize());
	}
	
	public static Path resolveAndCheck(Path basedir, Path other) {
		if (other.isAbsolute()) other = new File("/").toPath().relativize(other);
		return check(basedir, basedir.resolve(other).normalize());
	}
	
	private static Path check(Path basedir, Path path) {
		try {
			if (!path.startsWith(basedir) || Files.isSameFile(basedir, path)) throw runtime("invalid path [%s] - outside application route", path);
			return path;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

}
