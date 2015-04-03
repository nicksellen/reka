package reka.app;

import reka.api.Path;

public class PathAndVersion {
	
	public static PathAndVersion create(Path path, int version) {
		return new PathAndVersion(path, version);
	}
	
	private final Path path;
	private final int version;
	
	private PathAndVersion(Path path, int version) {
		this.path = path;
		this.version = version;
	}
	
	public Path path() {
		return path;
	}

	public int version() {
		return version;
	}
	
}
