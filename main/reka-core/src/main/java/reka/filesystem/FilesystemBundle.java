package reka.filesystem;

import static java.util.Arrays.asList;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.config.configurer.Configurer.configure;

import java.nio.file.Path;
import java.util.function.Function;

import reka.api.Path.Response;
import reka.api.data.Data;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.core.bundle.RekaBundle;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.util.StringWithVars;
import reka.nashorn.OperationsConfigurer;

public class FilesystemBundle implements RekaBundle {

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("filesystem"), () -> new FilesystemModule());
	}
	
	public static class FilesystemReadConfigurer implements OperationsConfigurer {

		private final Path basedir;
		
		private boolean download = false;
		
		private Function<Data,reka.api.Path> dataPathFn = (unused) -> Response.CONTENT;
		private Function<Data,String> filenameFn;
		
		public FilesystemReadConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.At("out")
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
			ops.add("files/read", store -> new FilesystemRead(basedir, dataPathFn, filenameFn, download));
		}
		
	}
	
	public static class FilesystemDeleteConfigurer implements OperationsConfigurer {

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
			ops.add("filesystem/delete", store -> new FilesystemDelete(basedir, filenameFn));
		}
		
	}
	
	public static class FilesystemListConfigurer implements OperationsConfigurer {

		private final Path basedir;
		
		private Function<Data,reka.api.Path> dataPathFn = (unused) -> Response.CONTENT;
		private Function<Data,String> dirFn = (unused) -> ".";
		
		public FilesystemListConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.At("out")
		public void data(String val) {
			dataPathFn = StringWithVars.compile(val).andThen(path -> dots(path));
		}

		@Conf.At("dir")
		public void filename(String val) {
			dirFn = StringWithVars.compile(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("files/list", store -> new FilesystemList(basedir, dataPathFn, dirFn));
		}
		
	}
	
	public static class FilesystemMktmpDirConfigurer implements OperationsConfigurer {
		
		private reka.api.Path dirname = path("tmpdir");
		
		@Conf.Val
		public void data(String val) {
			dirname = dots(val);
		}
		
		@Override
		public void setup(OperationSetup ops) {
			ops.add("files/mktmpdir", store -> new FilesystemMktempDir(dirname));
		}
		
	}

	public static class FilesystemTypeConfigurer implements OperationsConfigurer {

		private final ConfigurerProvider provider;

		private final Path basedir;
		
		private Function<Data,String> pathFn = (unused) -> ".";
		
		private OperationsConfigurer whenDir;
		private OperationsConfigurer whenFile;
		private OperationsConfigurer whenMissing;
		
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
		
		private OperationsConfigurer ops(ConfigBody body) {
			return configure(new SequenceConfigurer(provider), body);
		}

		@Override
		public void setup(OperationSetup ops) {
			ops.addRouter("file/type", store -> new FilesystemType(basedir, pathFn));
			ops.parallel(par -> {
				if (whenDir != null) par.route("dir", whenDir);
				if (whenFile != null) par.route("file", whenFile);
				if (whenMissing != null) par.route("missing", whenMissing);
			});
		}
		
	}

}
