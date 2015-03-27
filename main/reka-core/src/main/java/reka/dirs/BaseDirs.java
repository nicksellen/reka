package reka.dirs;

import static reka.api.Path.slashes;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import reka.util.Util;

public class BaseDirs extends AbstractDirs implements Dirs {
	
	private static final List<Path> tmpdirs = Collections.synchronizedList(new ArrayList<>());
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override public void run() {
				tmpdirs.forEach(Util::deleteRecursively);
			}
		});
	}
	
	public BaseDirs(String app, String data, String tmp) {
		this(asPath(app), asPath(data), asPath(tmp));
	}
	
	private BaseDirs(Path app, Path data, Path tmp) {
		super(app, data, tmp);
	}

	public AppDirs resolve(reka.api.Path appPath, int version) {
		Path tmpdir = tmp.resolve(AppDirs.dirnameFor(appPath));
		
		tmpdirs.add(tmpdir);
		
		return new AppDirs(appPath,
						   app.resolve(AppDirs.dirnameFor(appPath, version)), 
						   data.resolve(AppDirs.dirnameFor(appPath)), 
						   tmpdir, this);
	}

	public void delete(reka.api.Path appPath) {
		new AppDirs(appPath,
					app.resolve(AppDirs.dirnameFor(appPath)), 
				    data.resolve(AppDirs.dirnameFor(appPath)), 
				    tmp.resolve(AppDirs.dirnameFor(appPath)), this).delete();
	}
	
	public AppDirs mktemp() {
		String uuid = "tmp." + UUID.randomUUID().toString();
		reka.api.Path appPath = slashes(uuid);
		AppDirs dirs = new AppDirs(appPath, app.resolve(uuid), data.resolve(uuid), tmp.resolve(uuid), this);
		tmpdirs.add(dirs.app);
		tmpdirs.add(dirs.data);
		tmpdirs.add(dirs.tmp);
		return dirs;
	}

	private static Path asPath(String path) {
		return new File(path).toPath().toAbsolutePath();
	}
	
}
