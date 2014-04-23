package reka.test.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static reka.api.Path.dots;
import static reka.api.Path.path;
import static reka.api.Path.PathElements.index;
import static reka.api.Path.PathElements.name;

import java.util.Iterator;

import org.junit.Test;

import reka.api.Path;
import reka.api.Path.PathElement;

public class PathTest {
	
	@Test
	public void canBeMadeFromURL() {
		String url = "this%2Fpath/should%20be%20lovely%20and/fine/even/with%20spaces";
		Path path = Path.fromURL(url);
		
		Iterator<PathElement> it = path.iterator();
		assertThat(it.next().name(), equalTo("this/path"));
		assertThat(it.next().name(), equalTo("should be lovely and"));
		assertThat(it.next().name(), equalTo("fine"));
		
		assertThat(path.toURL(), equalTo(url));
	}
	
	@Test
	public void tryWrapping() {
		assertThat(dots("hello.here").wrapAndJoin("[ {} ]", " -> "), equalTo("[ hello ] -> [ here ]"));
	}
	
	@Test
	public void parsesArrayIndexes() {
		String path = "something.like[0].this";
		assertThat(dots(path).dots(), equalTo(path));
		assertThat(dots("something.like[0].this"), equalTo(
			path(name("something"), name("like"), index(0), name("this"))));
	}
	
	@Test
	public void youCanEscapeDots() {
		String str = "this.is\\.very.nice";
		Path path = path("this", "is.very", "nice");
		assertThat(dots(str), equalTo(path));
		assertThat(path.dots(), equalTo(str));
	}
	
	@Test
	public void ignoresMultipleDots() {
		assertThat(dots("what...about..this").dots(), equalTo("what.about.this"));
	}

}
