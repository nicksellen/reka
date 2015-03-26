package reka;

import static java.lang.String.format;

public class Identity {

	public static Identity create(String name) {
		return new Identity(name);
	}
	
	private final String name;
	
	private Identity(String name){
		this.name = name;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public String toString() {
		return format("%s[%d]", name, System.identityHashCode(this));
	}
	
}
