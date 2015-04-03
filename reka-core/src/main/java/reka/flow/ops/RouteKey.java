package reka.flow.ops;

import static java.lang.System.identityHashCode;

public class RouteKey {

	public static RouteKey named(String name) {
		return new RouteKey(name);
	}
	
	private final int id = identityHashCode(this);
	private final String name;
	
	private RouteKey(String name) {
		this.name = name;
	}
	
	public int id() {
		return id;
	}
	
	public String name() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
	
}