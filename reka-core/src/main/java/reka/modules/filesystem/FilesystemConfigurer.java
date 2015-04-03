package reka.modules.filesystem;

import static reka.api.Path.path;

import java.nio.file.Path;

import reka.config.configurer.annotations.Conf;
import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;
import reka.modules.filesystem.FilesystemModule.FilesystemDeleteConfigurer;
import reka.modules.filesystem.FilesystemModule.FilesystemListConfigurer;
import reka.modules.filesystem.FilesystemModule.FilesystemMktmpDirConfigurer;
import reka.modules.filesystem.FilesystemModule.FilesystemReadConfigurer;
import reka.modules.filesystem.FilesystemModule.FilesystemTypeConfigurer;

public class FilesystemConfigurer extends ModuleConfigurer {
	
	private String dir = "";
	
	@Conf.At("dir")
	public void basedir(String val) {
		if (val.startsWith("/")) val = val.substring(1);
		dir = val;
	}

	@Override
	public void setup(AppSetup module) {
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