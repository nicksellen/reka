package reka.filesystem;

import static java.util.Arrays.asList;

import java.io.File;
import java.nio.file.Path;

import reka.config.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.filesystem.FilesystemBundle.FilesystemDeleteConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemListConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemMktmpDirConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemReadConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemTypeConfigurer;

public class UseFilesystem extends UseConfigurer {
	
	private Path basedir = new File("/").toPath();
	
	@Conf.At("dir")
	public void basedir(String val) {
		basedir = new File(val).toPath();
	}

	@Override
	public void setup(UseInit init) {
		init.operation("write", () -> new FilesystemWriteConfigurer(basedir));
		init.operation("read", () -> new FilesystemReadConfigurer(basedir));
		init.operation(asList("list", "ls"), () -> new FilesystemListConfigurer(basedir));
		init.operation(asList("mktmpdir"), () -> new FilesystemMktmpDirConfigurer());
		init.operation(asList("delete", "rm"), () -> new FilesystemDeleteConfigurer(basedir));
		init.operation(asList("resolve", "expand", "full-path"), () -> new FilesystemResolveConfigurer(basedir));
		init.operation(asList("type", "switch"), (provider) -> new FilesystemTypeConfigurer(provider, basedir));
	}
	
}