package reka.gitfordata;

import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.root;
import static reka.api.Path.slashes;
import static reka.api.content.Contents.nonSerializableContent;
import static reka.api.content.Contents.utf8;
import static reka.core.builder.FlowSegments.sync;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;
import reka.api.Path.PathElements;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.data.storage.DataStorage;
import reka.api.data.storage.MemoryStorage;
import reka.api.data.versioned.VersionedAtomicMutableData.Mutation;
import reka.api.data.versioned.VersionedData.DataVersion;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleInit;
import reka.core.util.StringWithVars;
import reka.leveldb.LevelDBStorage;

public class GitForDataModule extends ModuleConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(GitForDataModule.class);
	
	private Path branchLocation = dots("default");
	
	private String leveldb;
	
	@Conf.At("branch")
	public void branch(String val) {
		branchLocation = dots(val);
	}
	
	@Conf.At("leveldb")
	public void leveldb(String val) {
		leveldb = val;
	}
	
	@Override
	public void setup(ModuleInit init) {

		Path repoPath = Path.path("stuff").add(init.path()).add("repo");
		Path branchPath = Path.path("stuff").add(init.path()).add("branch");
		
		init.init("prepare repo", (data) -> {
			
			DataStorage meta, objects;
			
			if (leveldb != null) {

				File leveldbDir = new File(leveldb);
				java.nio.file.Path leveldbPath = leveldbDir.toPath();
				try {
					Files.createDirectories(leveldbPath);
					meta = new LevelDBStorage(leveldbPath.resolve("meta.ldb").toFile().getAbsolutePath());
					objects = new LevelDBStorage(leveldbPath.resolve("objects.ldb").toFile().getAbsolutePath());
				} catch (IOException e) {
					throw unchecked(e);
				}
			} else {
				meta = new MemoryStorage();
				objects = new MemoryStorage();
			}
			
			DataRepo repo = new DataRepo(meta, objects);
			DataRepoBranch branch = repo.branch(branchLocation);
			data.put(repoPath, nonSerializableContent(repo));
			data.put(branchPath, nonSerializableContent(branch));
			return data;
		});
		
		init.operation(path("put"), () -> new PutConfigurer(branchPath));
		init.operation(path("delete"), () -> new DeleteConfigurer(branchPath));
		init.operation(path("get"), () -> new GetConfigurer(branchPath));
		init.operation(path("keys"), () -> new KeysConfigurer(branchPath));
		init.operation(path("list"), () -> new ListConfigurer(branchPath));
	}
	
	public static DataRepoBranch branch(Data data, Path branchPath) {
		return data.getContent(branchPath).get().valueAs(DataRepoBranch.class);
	}
	
	public static class PutConfigurer implements Supplier<FlowSegment> {

		private final Path branchPath;
		
		public PutConfigurer(Path branchPath) {
			this.branchPath = branchPath;
		}

		private Path in = root();
		private Function<Data,String> fieldPath = StringWithVars.compile("");
		
		@Conf.At("path")
		public void path(String val) {
			fieldPath = StringWithVars.compile(val);
		}
		
		@Conf.At("in")
		public void in(String val) {
			in = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("put", (data) -> new PutOperation(
					branch(data, branchPath), 
					fieldPath, in));
		}
		
	}
	
	public static class GetConfigurer implements Supplier<FlowSegment> {

		private final Path branchPath;
		
		private Function<Data,String> versionFn;
		
		public GetConfigurer(Path branchPath) {
			this.branchPath = branchPath;
		}
		
		private Path out = root();
		private Function<Data,String> fieldFn = StringWithVars.compile("");
		
		
		@Conf.At("path")
		public void path(String val) {
			fieldFn = StringWithVars.compile(val);
		}

		@Conf.At("version")
		public void versionPath(String val) {
			versionFn = StringWithVars.compile(val);
		}
		
		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			if (versionFn != null) {
				return sync("get", (data) -> new GetWithVersionOperation(
						branch(data, branchPath), 
						fieldFn, versionFn, out));
			} else {
				return sync("get", (data) -> new GetOperation(
						branch(data, branchPath), 
						fieldFn, out));
			}
		}
		
	}
	
	public static class DeleteConfigurer implements Supplier<FlowSegment> {

		private final Path branchPath;
		
		public DeleteConfigurer(Path branchPath) {
			this.branchPath = branchPath;
		}
		
		private Function<Data,String> fieldFn = StringWithVars.compile("");
		
		@Conf.At("path")
		public void path(String val) {
			fieldFn = StringWithVars.compile(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("get", (data) -> new DeleteOperation(branch(data, branchPath), fieldFn));
		}
		
	}
	
	public static class KeysConfigurer implements Supplier<FlowSegment> {

		private final Path branchPath;
		
		public KeysConfigurer(Path branchPath) {
			this.branchPath = branchPath;
		}
		
		private Path out = root();
		private Function<Data,String> fieldFn = StringWithVars.compile("");
		
		@Conf.At("path")
		public void path(String val) {
			fieldFn = StringWithVars.compile(val);
		}
		
		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}
		
		@Override
		public FlowSegment get() {
			return sync("keys", (data) -> new KeysOperation(
					branch(data, branchPath), 
					fieldFn, out));
		}
		
	}
	
	public static class ListConfigurer implements Supplier<FlowSegment> {

		private final Path branchPath;
		
		private final List<Function<Data,Path>> fieldFns = new ArrayList<>();
		
		private Path out = root();
		private Function<Data,String> fieldFn = StringWithVars.compile("");
		
		public ListConfigurer(Path branchPath) {
			this.branchPath = branchPath;
		}
		
		@Conf.At("path")
		public void path(String val) {
			fieldFn = StringWithVars.compile(val);
		}
		
		@Conf.At("out")
		public void out(String val) {
			out = dots(val);
		}
		
		@Conf.Each("field")
		public void field(String val) {
			Function<Data,Path> fn = StringWithVars.compile(val).andThen(Path::slashes);
			fieldFns.add(fn);
		}
		
		@Override
		public FlowSegment get() {
			return sync("keys", (data) -> new ListOperation(
					branch(data, branchPath), 
					fieldFn, fieldFns, out));
		}
		
	}
	
	private static final Path commitVersionPath = dots("commit.version");
	
	public static class PutOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Function<Data,String> field;
		private final Path in;
		
		public PutOperation(DataRepoBranch branch, Function<Data,String> field, Path in) {
			this.branch = branch;
			this.field = field;
			this.in = in;
		}
		
		@Override
		public MutableData call(MutableData data) {
			
			Mutation mutation = branch.latest().createMutation();
			
			Path fieldPath = slashes(field.apply(data));
			
			data.atFirst(in.add("data"), in).forEachContent((path, content) -> {
				log.debug("added {} ({}) to mutatino", content, content.getClass());
				mutation.put(fieldPath.add(path), content);
			});
			
			try {
				DataVersion version = mutation.commit().get();
				data.putString(commitVersionPath, version.text());
			} catch (InterruptedException | ExecutionException e) {
				throw unchecked(e);
			}
			
			return data;
		}
		
	}
	
	public static class GetOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Path out;
		private final Function<Data,String> field;
		
		public GetOperation(DataRepoBranch branch, Function<Data,String> field, Path out) {
			this.branch = branch;
			this.field = field;
			this.out = out;
		}
		
		@Override
		public MutableData call(MutableData data) {
			Path fieldPath = slashes(field.apply(data));
			branch.latest().at(fieldPath).forEachContent((path, content) -> {
				data.put(out.add(path), content);
			});
			return data;
		}
		
	}
	
	public static class DeleteOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Function<Data,String> field;
		
		public DeleteOperation(DataRepoBranch branch, Function<Data,String> field) {
			this.branch = branch;
			this.field = field;
		}
		
		@Override
		public MutableData call(MutableData data) {
			Path fieldPath = slashes(field.apply(data));
			try {
				branch.latest().createMutation()
						.remove(fieldPath)
					.commit().get();
			} catch (InterruptedException | ExecutionException e) {
				throw unchecked(e);
			}
			return data;
		}
		
	}
	
	public static class GetWithVersionOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Path out;
		private final Function<Data,String> fieldFn;
		private final Function<Data,String> versionFn;
		
		public GetWithVersionOperation(DataRepoBranch branch, Function<Data,String> fieldFn, Function<Data,String> versionFn, Path out) {
			this.branch = branch;
			this.fieldFn = fieldFn;
			this.versionFn = versionFn;
			this.out = out;
			
		}
		
		@Override
		public MutableData call(MutableData data) {
			Path field = slashes(fieldFn.apply(data));
			SHA1DataVersion version = SHA1DataVersion.fromText(versionFn.apply(data));
			branch.atVersion(version).at(field).forEachContent((path, content) -> {
				data.put(out.add(path), content);
			});
			return data;
		}
		
	}
	
	public static class KeysOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Path out;
		private final Function<Data,String> field;
		
		public KeysOperation(DataRepoBranch branch, Function<Data,String> field, Path out) {
			this.branch = branch;
			this.field = field;
			this.out = out;
		}
		
		@Override
		public MutableData call(MutableData data) {
			Path fieldPath = slashes(field.apply(data));
			
			MutableData list = data.createListAt(out);
			for (PathElement e : branch.latest().at(fieldPath).elements()) {
				list.put(PathElements.nextIndex(), utf8(e.toString()));
			}
			
			return data;
		}
		
	}
	
	public static class ListOperation implements SyncOperation {

		private final DataRepoBranch branch;
		private final Path out;
		private final Function<Data,String> pathFn;
		private final List<Function<Data,Path>> fieldFns;
		
		public ListOperation(DataRepoBranch branch, Function<Data,String> pathFn, List<Function<Data,Path>> fieldFns, Path out) {
			this.branch = branch;
			this.pathFn = pathFn;
			this.fieldFns = fieldFns;
			this.out = out;
		}
		
		@Override
		public MutableData call(MutableData data) {
			Path path = slashes(pathFn.apply(data));

			// ensures we have an empty list if there are no results
			// TODO: fixup implementation so we can create lists and add maps to them etc
			data.createListAt(out);
			
			branch.latest().at(path).forEachData((e, d) -> {
				Path base = out.add(e);
				fieldFns.forEach(fn -> {
					Path field = fn.apply(data);
					d.at(field).forEachContent((p, c) -> {
						data.put(base.add(field).add(p), c);
					});
				});
			});
			/*
			for (PathElement e : at.elements()) {
				obj = at.at(e);
				//list.put(PathElements.nextIndex(), utf8(e.toString()));
				list.createMapAt(nextIndex());
				
				
				list.putMap(nextIndex(), map -> {
					fieldFns.forEach(fn -> {
						Path field = fn.apply(data);
						obj.at(field).forEachContent((p, c) -> {
							map.put(field.add(p), c);
						});
					});
				});
			}
			
			*/
			return data;
		}
		
	}
	
}
