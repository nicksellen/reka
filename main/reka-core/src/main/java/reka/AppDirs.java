package reka;

import java.nio.file.Path;

public class AppDirs extends AbstractDirs {
	
	public AppDirs(Path app, Path data, Path tmp, BaseDirs basedirs) {
		super(app, data, tmp);
		this.basedirs = basedirs;
	}

	private final BaseDirs basedirs;
		
	public BaseDirs basedirs() {
		return basedirs;
	}
	
}
