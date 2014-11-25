package reka;

import static reka.util.Util.encode64;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class BaseDirs extends AbstractDirs {
	
	public BaseDirs(String app, String data, String tmp) {
		this(asPath(app), asPath(data), asPath(tmp));
	}
	
	public BaseDirs(Path app, Path data, Path tmp) {
		super(app, data, tmp);
	}

	public AppDirs resolve(String other) {
		return resolveInternal(encode64(other));
	}
	
	public AppDirs mktemp() {
		AppDirs dirs = resolveInternal("tmp." + UUID.randomUUID().toString());
		dirs.app.toFile().deleteOnExit();
		dirs.data.toFile().deleteOnExit();
		dirs.tmp.toFile().deleteOnExit();
		return dirs;
	}
	
	private AppDirs resolveInternal(String other) {
		return new AppDirs(app.resolve(other), data.resolve(other), tmp.resolve(other), this);
	}

	private static Path asPath(String path) {
		return new File(path).toPath();
	}
	
}
