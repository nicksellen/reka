package reka.filesystem;

import static java.util.Arrays.asList;
import static reka.api.Path.path;

import java.io.File;
import java.nio.file.Path;

import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;
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
	public void setup(ModuleSetup module) {
		module.operation(path("write"), () -> new FilesystemWriteConfigurer(basedir));
		module.operation(path("read"), () -> new FilesystemReadConfigurer(basedir));
		module.operation(asList(path("list"), path("ls")), () -> new FilesystemListConfigurer(basedir));
		module.operation(asList(path("mktmpdir")), () -> new FilesystemMktmpDirConfigurer());
		module.operation(asList(path("delete"), path("rm")), () -> new FilesystemDeleteConfigurer(basedir));
		module.operation(asList(path("resolve"), path("expand"), path("full-path")), () -> new FilesystemResolveConfigurer(basedir));
		module.operation(asList(path("type"), path("switch")), (provider) -> new FilesystemTypeConfigurer(provider, basedir));
	}
	
}