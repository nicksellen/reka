package reka.util.dirs;

import java.nio.file.Path;

public interface Dirs {
	Path app();
	Path data();
	Path tmp();
	void delete();
	void mkdirs();	
}
