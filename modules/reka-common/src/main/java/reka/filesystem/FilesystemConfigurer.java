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
		module.defineOperation(path("write"), provider -> new FilesystemWriteConfigurer(datadir));
		module.defineOperation(path("list"), provider -> new FilesystemListConfigurer(datadir));
		module.defineOperation(path("read"), provider -> new FilesystemReadConfigurer(datadir));
		module.defineOperation(path("ls"), provider -> new FilesystemListConfigurer(datadir));
		module.defineOperation(path("mktmpdir"), provider -> new FilesystemMktmpDirConfigurer(tmpdir));
		module.defineOperation(path("delete"), provider -> new FilesystemDeleteConfigurer(datadir));
		module.defineOperation(path("rm"), provider -> new FilesystemDeleteConfigurer(datadir));
		module.defineOperation(path("resolve"), provider -> new FilesystemResolveConfigurer(datadir));
		module.defineOperation(path("full-path"), provider -> new FilesystemResolveConfigurer(datadir));
		module.defineOperation(path("expand"), provider -> new FilesystemResolveConfigurer(datadir));
		module.defineOperation(path("type"), provider -> new FilesystemTypeConfigurer(provider, datadir));
		module.defineOperation(path("switch"), provider -> new FilesystemTypeConfigurer(provider, datadir));
	}
	
}