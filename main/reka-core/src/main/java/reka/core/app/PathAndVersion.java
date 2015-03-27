package reka.core.app;

import static reka.api.Path.slashes;

import java.util.UUID;

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

	public static PathAndVersion tmp() {
		return PathAndVersion.create(slashes(String.format("tmp/%s", UUID.randomUUID().toString())), 1);
	}
	
}
