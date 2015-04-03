package reka.modules.filesystem;

import static java.util.Arrays.asList;
import static reka.config.configurer.Configurer.configure;
import static reka.util.Path.dots;
import static reka.util.Path.path;

import java.nio.file.Path;
import java.util.function.Function;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.data.Data;
import reka.module.Module;
import reka.module.ModuleDefinition;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;
import reka.util.StringWithVars;
import reka.util.Path.Response;

public class FilesystemModule implements Module {

	@Override
	public reka.util.Path base() {
		return path("fs");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new FilesystemConfigurer());
	}
	
	public static class FilesystemReadConfigurer implements OperationConfigurer {

		private final Path basedir;
		
		private boolean download = false;
		
		private Function<Data,reka.util.Path> dataPathFn = (unused) -> Response.CONTENT;
		private Function<Data,String> filenameFn;
		
		public FilesystemReadConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.At("out")
		@Conf.At("into")
		public void data(String val) {
			dataPathFn = StringWithVars.compile(val).andThen(path -> dots(path));
		}
		
		@Conf.At("filename")
		public void filename(String val) {
			filenameFn = StringWithVars.compile(val);
		}

		@Conf.At("download")
		public void download(String val) {
			download = !asList("false", "no").contains(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("read", () -> new FilesystemRead(basedir, dataPathFn, filenameFn, download));
		}
		
	}
	
	public static class FilesystemDeleteConfigurer implements OperationConfigurer {

		private final Path basedir;
		
		private Function<Data,String> filenameFn;
		
		public FilesystemDeleteConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.Val
		@Conf.At("filename")
		public void filename(String val) {
			filenameFn = StringWithVars.compile(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("delete", () -> new FilesystemDelete(basedir, filenameFn));
		}
		
	}
	
	public static class FilesystemListConfigurer implements OperationConfigurer {

		private final Path basedir;
		
		private Function<Data,reka.util.Path> dataPathFn = (unused) -> Response.CONTENT;
		private Function<Data,String> dirFn = (unused) -> ".";
		
		public FilesystemListConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.At("out")
		@Conf.At("into")
		public void data(String val) {
			dataPathFn = StringWithVars.compile(val).andThen(path -> dots(path));
		}

		@Conf.At("dir")
		public void filename(String val) {
			dirFn = StringWithVars.compile(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("list", () -> new FilesystemList(basedir, dataPathFn, dirFn));
		}
		
	}
	
	public static class FilesystemMktmpDirConfigurer implements OperationConfigurer {
		
		private final Path tmpdir;
		
		public FilesystemMktmpDirConfigurer(Path tmpdir) {
			this.tmpdir = tmpdir;
		}
		
		private reka.util.Path dirname = path("tmpdir");
		
		@Conf.Val
		public void data(String val) {
			dirname = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("mktmpdir", () -> new FilesystemMktempDir(tmpdir, dirname));
		}
		
	}

	public static class FilesystemTypeConfigurer implements OperationConfigurer {
		
		private final ConfigurerProvider provider;

		private final Path basedir;
		
		private Function<Data,String> pathFn = (unused) -> ".";
		
		private OperationConfigurer whenDir;
		private OperationConfigurer whenFile;
		private OperationConfigurer whenMissing;
		
		public FilesystemTypeConfigurer(ConfigurerProvider provider, Path basedir) {
			this.provider = provider;
			this.basedir = basedir;
		}
		
		@Conf.Val
		@Conf.At("path")
		public void data(String val) {
			pathFn = StringWithVars.compile(val);
		}
		
		@Conf.Each("when")
		public void when(Config config) {
			switch (config.valueAsString()) {
			case "dir":
				whenDir = ops(config.body());
				break;
			case "file":
				whenFile = ops(config.body());
				break;
			case "missing":
				whenMissing = ops(config.body());
				break;
			}
		}
		
		private OperationConfigurer ops(ConfigBody body) {
			return configure(new SequenceConfigurer(provider), body);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.router("type", () -> new FilesystemType(basedir, pathFn), router -> {
				if (whenDir != null) router.add(FilesystemType.DIR, whenDir);
				if (whenFile != null) router.add(FilesystemType.FILE, whenFile);
				if (whenMissing != null) router.add(FilesystemType.MISSING, whenMissing);
			});
		}
		
	}

}
