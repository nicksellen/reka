package reka.gitfordata;

import static reka.api.Path.dots;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.content.Content;
import reka.api.data.storage.DataStorage;
import reka.api.data.storage.MemoryStorage;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.api.data.versioned.VersionedData.DataVersion;
import reka.gitfordata.tree.ObjectId;
import reka.gitfordata.tree.RecordReader;

public class DataRepo {
	
	private static final Logger log = LoggerFactory.getLogger(DataRepo.class);
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		DataRepoBranch branch = new DataRepo(new MemoryStorage(), new MemoryStorage()).branch(dots("nick.branches.master"));
		
		VersionedAtomicMutableData data = branch.latest();
		
		//log.debug("starting at version", ObjectId.fromBytes(data.version().id()).hex());

		DataVersion nickVersion = data.createMutation()
				.putString(dots("people.nick.name"), "nick")
			.commit().get();

		data = branch.latest();
		

		log.debug("now at version [{}]", ObjectId.fromBytes(data.version().id()).hex());
		
		data.forEachContent((path, content) -> {
			log.debug("visited: {}: {}", path.dots(), content);
		});

		data.createMutation()
				.putString(dots("people.nick.name"), "peter")
			.commit().get();

		data = branch.latest();

		log.debug("now at version [{}]", ObjectId.fromBytes(data.version().id()).hex());
		
		Optional<Content> c = data.getContent(dots("people.nick.name"));
		
		log.debug("c?: {}", c.isPresent());
		
		log.debug("got: {}", data.getString(dots("people.nick.name")).orElse("meh"));
		
		branch.atVersion(nickVersion).forEachContent((path, content) -> {
			log.debug("nv: {} -> {}", path, content);
		});
		
	}
	
	protected final DataStorage meta;
	protected final DataStorage objects;
	protected final RecordReader reader;
	
	public DataRepo(DataStorage meta, DataStorage objects) {
		this.meta = meta;
		this.objects = objects;
		this.reader = new RecordReader(objects);
	}
	
	public DataRepoBranch branch(Path location) {
		return new DataRepoBranch(this, location);
	}
	
}
