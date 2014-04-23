package nicksellen.flow.util;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.PathElement;

public class PathTest {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	public void testbutlast() {
		Path path = Path.dots("lots.of.things.here");
		assertThat(path.butlast(), equalTo(Path.dots("lots.of.things")));
	}
	
	@Test
	public void testDifferentCreationMethods() {
		
		String original = "example-path-here";
		Path a = Path.path("example", "path", "here");
		Path b = Path.dots("example.path.here");
		Path c = Path.slashes("example/path/here");
		
		assertThat(a.join("-"), equalTo(original));
		assertThat(b.join("-"), equalTo(original));
		assertThat(c.join("-"), equalTo(original));
		
	}
	
	@Test
	public void canAddThings() {
		Path a = Path.path("things", "here");
		Path b = a.add("yay");
		
		assertThat(a.dots(), equalTo("things.here"));
		assertThat(b.dots(), equalTo("things.here.yay"));
	}
	
	@Test
	public void acceptsNumbersToo() {
		Path path = Path.newBuilder()
			.add("nick")
			.add(5)
			.add("woah").build();
		
		log.debug("with index {}\n", path.dots());
		
		Path parsed = Path.dots(path.dots());
		
		for (PathElement e : parsed) {
			log.debug("e: {} ({})", e, e.isKey() ? "name" : "index");
		}
	}
	
	@Test
	public void toHexAndBack() {
		String hex = Path.dots("a.lovely.path.like.this").hex();
		
		Path path = Path.fromHex(hex);
		
		log.debug("path [{}] as hex is [{}]", path.dots(), hex);
		
		assertThat(path, equalTo(Path.path("a", "lovely", "path", "like", "this")));
	}

}
