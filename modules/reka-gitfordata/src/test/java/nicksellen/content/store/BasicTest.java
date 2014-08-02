package nicksellen.content.store;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import reka.api.data.storage.MemoryStorage;
import reka.api.data.versioned.VersionedAtomicMutableData;
import reka.gitfordata.DataRepo;
import reka.gitfordata.DataRepoBranch;

public class BasicTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {

    	DataRepoBranch branch = new DataRepo(new MemoryStorage(), new MemoryStorage()).branch(path("bom"));
    	
        VersionedAtomicMutableData boo = branch.latest();

        boo.createMutation()
                .put(dots("people.nick.name"), utf8("nick"))
                .put(dots("people.nick.age"), integer(29))
             .commit().get();
        
        boo = branch.latest();

        assertThat(boo.getString(dots("people.nick.name")).orElse("not found"), equalTo("nick"));
        assertThat(boo.getInt(dots("people.nick.age")).orElse(-1), equalTo(29));

    }

}

