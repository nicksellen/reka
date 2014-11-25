package reka.filesystem;

import static reka.api.Path.path;

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
	
	private String dir = "";
	
	@Conf.At("dir")
	public void basedir(String val) {
		if (val.startsWith("/")) val = val.substring(1);
		dir = val;
	}

	@Override
	public void setup(ModuleSetup module) {
		Path datadir = dirs().data().resolve(dir);
		Path tmpdir = dirs().tmp().resolve(dir);
		module.operation(path("write"), provider -> new FilesystemWriteConfigurer(datadir));
		module.operation(path("list"), provider -> new FilesystemListConfigurer(datadir));
		module.operation(path("read"), provider -> new FilesystemReadConfigurer(datadir));
		module.operation(path("ls"), provider -> new FilesystemListConfigurer(datadir));
		module.operation(path("mktmpdir"), provider -> new FilesystemMktmpDirConfigurer(tmpdir));
		module.operation(path("delete"), provider -> new FilesystemDeleteConfigurer(datadir));
		module.operation(path("rm"), provider -> new FilesystemDeleteConfigurer(datadir));
		module.operation(path("resolve"), provider -> new FilesystemResolveConfigurer(datadir));
		module.operation(path("full-path"), provider -> new FilesystemResolveConfigurer(datadir));
		module.operation(path("expand"), provider -> new FilesystemResolveConfigurer(datadir));
		module.operation(path("type"), provider -> new FilesystemTypeConfigurer(provider, datadir));
		module.operation(path("switch"), provider -> new FilesystemTypeConfigurer(provider, datadir));
	}
	
}