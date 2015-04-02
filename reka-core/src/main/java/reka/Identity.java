package reka;

import static java.lang.String.format;
import static reka.api.Path.slashes;
import reka.api.Path;

public class Identity {

	public static Identity create(String name) {
		return new Identity(name);
	}
	
	private final String name;
	private final Path path;
	private final String toString;
	
	private Identity(String name){
		this.name = name;
		this.path = slashes(name);
		this.toString = format("%s[%d]", name, System.identityHashCode(this));
	}
	
	public String name() {
		return name;
	}
	
	public Path path() {
		return path;
	}
	
	@Override
	public String toString() {
		return toString;
	}
	
}
