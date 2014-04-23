package reka.filesystem;

import static java.util.Arrays.asList;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.configurer.Configurer.configure;
import static reka.core.builder.FlowSegments.sequential;
import static reka.core.builder.FlowSegments.sync;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.configurer.annotations.Conf;
import reka.core.bundle.RekaBundle;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.util.StringWithVars;

public class FilesystemBundle implements RekaBundle {

	@Override
	public void setup(Setup setup) {
		setup.use(path("filesystem"), () -> new UseFilesystem());
	}
	
	public static class FilesystemReadConfigurer implements Supplier<FlowSegment> {

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
		public FlowSegment get() {
			return sync("files/read", () -> new FilesystemRead(basedir, dataPathFn, filenameFn, download));
		}
		
	}
	
	public static class FilesystemDeleteConfigurer implements Supplier<FlowSegment> {

		private final Path basedir;
		
		private Function<Data,String> filenameFn;
		
		public FilesystemDeleteConfigurer(Path basedir) {
			this.basedir = basedir;
		}
		
		@Conf.At("filename")
		public void filename(String val) {
			filenameFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("filesystem/delete", () -> new FilesystemDelete(basedir, filenameFn));
		}
		
	}
	
	public static class FilesystemListConfigurer implements Supplier<FlowSegment> {

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
		public FlowSegment get() {
			return sync("files/list", () -> new FilesystemList(basedir, dataPathFn, dirFn));
		}
		
	}

	public static class FilesystemTypeConfigurer implements Supplier<FlowSegment> {

		private final ConfigurerProvider provider;

		private final Path basedir;
		
		private Function<Data,String> pathFn = (unused) -> ".";
		
		private Supplier<FlowSegment> whenDir;
		private Supplier<FlowSegment> whenFile;
		private Supplier<FlowSegment> whenMissing;
		
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
		
		private Supplier<FlowSegment> ops(ConfigBody body) {
			return configure(new SequenceConfigurer(provider), body);
		}

		@Override
		public FlowSegment get() {
			return sequential(seq -> {
				seq.routerNode("file/type", (unused) -> new FilesystemType(basedir, pathFn));
				seq.parallel(par -> {
					if (whenDir != null) par.add("dir", whenDir.get());
					if (whenFile != null) par.add("file", whenFile.get());
					if (whenMissing != null) par.add("missing", whenMissing.get());
				});
			});
		}
		
	}

}
