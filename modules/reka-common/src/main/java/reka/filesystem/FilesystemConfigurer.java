package reka.filesystem;

import static reka.api.Path.path;

import java.io.File;
import java.nio.file.Path;

import reka.config.configurer.annotations.Conf;
import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;
import reka.filesystem.FilesystemModule.FilesystemDeleteConfigurer;
import reka.filesystem.FilesystemModule.FilesystemListConfigurer;
import reka.filesystem.FilesystemModule.FilesystemMktmpDirConfigurer;
import reka.filesystem.FilesystemModule.FilesystemReadConfigurer;
import reka.filesystem.FilesystemModule.FilesystemTypeConfigurer;

public class FilesystemConfigurer extends ModuleConfigurer {
	
	private Path basedir = new File("/").toPath();
	
	@Conf.At("dir")
	public void basedir(String val) {
		basedir = new File(val).toPath();
	}

	@Override
	public void setup(ModuleSetup module) {
		module.operation(path("write"), provider -> new FilesystemWriteConfigurer(basedir));
		module.operation(path("read"), provider -> new FilesystemReadConfigurer(basedir));
		module.operation(path("list"), provider -> new FilesystemListConfigurer(basedir));
		module.operation(path("ls"), provider -> new FilesystemListConfigurer(basedir));
		module.operation(path("mktmpdir"), provider -> new FilesystemMktmpDirConfigurer());
		module.operation(path("delete"), provider -> new FilesystemDeleteConfigurer(basedir));
		module.operation(path("rm"), provider -> new FilesystemDeleteConfigurer(basedir));
		module.operation(path("resolve"), provider -> new FilesystemResolveConfigurer(basedir));
		module.operation(path("full-path"), provider -> new FilesystemResolveConfigurer(basedir));
		module.operation(path("expand"), provider -> new FilesystemResolveConfigurer(basedir));
		module.operation(path("type"), provider -> new FilesystemTypeConfigurer(provider, basedir));
		module.operation(path("switch"), provider -> new FilesystemTypeConfigurer(provider, basedir));
	}
	
}