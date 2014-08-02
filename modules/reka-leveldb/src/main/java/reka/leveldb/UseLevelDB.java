package reka.leveldb;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.sync;

import java.util.function.Function;
import java.util.function.Supplier;

import reka.api.Path;
import reka.api.content.Contents;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowSegment;
import reka.api.run.SyncOperation;
import reka.configurer.annotations.Conf;
import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;
import reka.core.util.StringWithVars;

import com.google.common.base.Charsets;

public class UseLevelDB extends UseConfigurer {

	private String path;
	
	@Conf.At("db")
	public void path(String val) {
		path = val;
	}
	
	@Override
	public void setup(UseInit use) {
		
		Path dbPath = use.path().add("db");
		
		use.run("open or create db", (data) ->  {
			LevelDBStorage db = new LevelDBStorage(path);
			data.put(dbPath, Contents.nonSerializableContent(db));
			return data;
		});
		
		use.operation("put", () -> new PutConfigurer(dbPath));
		use.operation("get", () -> new GetConfigurer(dbPath));
		
	}
	
	public static class PutConfigurer implements Supplier<FlowSegment> {
		
		private final Path dbPath;
		
		private Function<Data,String> key, val;
		
		public PutConfigurer(Path dbPath) {
			this.dbPath = dbPath;
		}
		
		@Conf.At("key")
		public void key(String v) {
			key = StringWithVars.compile(v);
		}
		
		@Conf.At("value")
		public void value(String v) {
			val = StringWithVars.compile(v);
		}

		@Override
		public FlowSegment get() {
			return sync("put", (data) -> new Put(data.getContent(dbPath).get().valueAs(LevelDBStorage.class), key, val));
		}
		
	}
	
	public static class GetConfigurer implements Supplier<FlowSegment> {
		
		private final Path dbPath;
		
		private Function<Data,String> key;
		private Function<Data,Path> out;
		
		public GetConfigurer(Path dbPath) {
			this.dbPath = dbPath;
		}
		
		@Conf.At("key")
		public void key(String v) {
			key = StringWithVars.compile(v);
		}
		
		@Conf.At("out")
		@Conf.Val
		public void out(String v) {
			out = StringWithVars.compile(v).andThen(s -> dots(s));
		}

		@Override
		public FlowSegment get() {
			return sync("put", (data) -> new Get(data.getContent(dbPath).get().valueAs(LevelDBStorage.class), key, out));
		}
		
	}
	
	public static class Put implements SyncOperation {

		private final LevelDBStorage db;
		private final Function<Data,String> key, val;
		
		public Put(LevelDBStorage db, Function<Data,String> key, Function<Data,String> val) {
			this.db = db;
			this.key = key;
			this.val = val;
		}
		
		@Override
		public MutableData call(MutableData data) {
			String k = key.apply(data);
			String v = val.apply(data);
			db.createCommit()
					.add(k.getBytes(Charsets.UTF_8), v.getBytes(Charsets.UTF_8))
				.commit();
			return data;
		}
		
	}
	
	public static class Get implements SyncOperation {

		private final LevelDBStorage db;
		private final Function<Data,String> key;
		private final Function<Data,Path> out;
		
		public Get(LevelDBStorage db, Function<Data,String> key, Function<Data,Path> out) {
			this.db = db;
			this.key = key;
			this.out = out;
		}
		
		@Override
		public MutableData call(MutableData data) {
			String k = key.apply(data);
			String v = new String(db.get(k.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
			return data.putString(out.apply(data), v);
		}
		
	}

}
