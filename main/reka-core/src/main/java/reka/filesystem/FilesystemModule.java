package reka.filesystem;

import static java.util.Arrays.asList;
import static reka.api.Path.path;

import java.io.File;
import java.nio.file.Path;

import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;
import reka.filesystem.FilesystemBundle.FilesystemDeleteConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemListConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemMktmpDirConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemReadConfigurer;
import reka.filesystem.FilesystemBundle.FilesystemTypeConfigurer;

public class FilesystemModule extends ModuleConfigurer {
	
	private Path basedir = new File("/").toPath();
	
	@Conf.At("dir")
	public void basedir(String val) {
		basedir = new File(val).toPath();
	}

	@Override
	public void setup(ModuleInit init) {
		init.operation(path("write"), () -> new FilesystemWriteConfigurer(basedir));
		init.operation(path("read"), () -> new FilesystemReadConfigurer(basedir));
		init.operation(asList(path("list"), path("ls")), () -> new FilesystemListConfigurer(basedir));
		init.operation(asList(path("mktmpdir")), () -> new FilesystemMktmpDirConfigurer());
		init.operation(asList(path("delete"), path("rm")), () -> new FilesystemDeleteConfigurer(basedir));
		init.operation(asList(path("resolve"), path("expand"), path("full-path")), () -> new FilesystemResolveConfigurer(basedir));
		init.operation(asList(path("type"), path("switch")), (provider) -> new FilesystemTypeConfigurer(provider, basedir));
	}
	
}