package nicksellen.content.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.util.Util.unchecked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import reka.api.Path;
import reka.api.data.storage.SynchronizedMemoryStorage;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.api.data.versioned.VersionedData.DataVersion;
import reka.gitfordata.DataRepo;
import reka.gitfordata.DataRepoBranch;

public class HashedDatasetTest {
	
	@Test
	public void snapshots() throws InterruptedException, ExecutionException {
		
		DataRepo repo = new DataRepo(new SynchronizedMemoryStorage(), new SynchronizedMemoryStorage());
		
		DataRepoBranch branch = repo.branch(path("main"));
		
		VersionedAtomicMutableData store = branch.latest();
		
		final int threads = 20;
		final int times = 30;
		final CountDownLatch start = new CountDownLatch(threads);
		final CountDownLatch finish = new CountDownLatch(threads);

		final Path path = path(name("people"), name("nick"), name("name"));
		
		DataVersion originalVersion = store.createMutation()
				.putString(path, "Nick Original")
			.commit().get();
		
		for (int t = 0; t < threads; t++) {
		
			new Thread() {
				public void run() {
					try {
						start.countDown();
						start.await();
						for (int i = 0; i < times; i++) {
							String name = "nick " + i;
							
							DataVersion snapshotVersion = store.createMutation()
									.putString(path, name)
								.commit().get();
							
							store.createMutation()
									.remove(path)
								.commit().get();

							assertThat(branch.atVersion(originalVersion).getString(path).orElse("fail!"), equalTo("Nick Original"));
							assertThat(branch.atVersion(snapshotVersion).getString(path).orElse("oops"), equalTo(name));
							
						}
						finish.countDown();
					} catch (Throwable t) {
						throw unchecked(t);
					}
				}
			}.start();
		
		}
		
		finish.await();
		
	}

}
