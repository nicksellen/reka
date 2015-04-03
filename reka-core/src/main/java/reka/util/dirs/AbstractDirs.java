package reka.util.dirs;

import static reka.util.Util.deleteRecursively;
import static reka.util.Util.runtime;

import java.io.File;
import java.nio.file.Path;

public abstract class AbstractDirs implements Dirs {
	
	protected final Path app;
	protected final Path data;
	protected final Path tmp;
	
	protected AbstractDirs(Path app, Path data, Path tmp) {
		this.app = app;
		this.data = data;
		this.tmp = tmp;
	}
	
	public Path app() {
		return app;
	}
	
	public Path data() {
		return data;
	}
	
	public Path tmp() {
		return tmp;
	}
	
	public void delete() {
		deleteRecursively(app);
		deleteRecursively(data);
		deleteRecursively(tmp);
	}

	public void mkdirs() {
		mkdirs(app);
		mkdirs(data);
		mkdirs(tmp);
	}
	
	private void mkdirs(Path path) {
		File f = path.toFile();
		if (!f.isDirectory() && !f.mkdirs()) throw runtime("couldn't create dir %s", path);
	}
}
