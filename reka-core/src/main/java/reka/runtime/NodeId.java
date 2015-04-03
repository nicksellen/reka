package reka.runtime;

import java.util.Objects;

public class NodeId {
	private final String value;
	private NodeId(String value) {
		this.value = value;
	}
	public String value() {
		return value;
	}
	public String toString() {
		return value;
	}
	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		} else if (other instanceof NodeId) {
			return ((NodeId)other).value.equals(value);
		}
		return false;
	}
	public static NodeId fromString(String value) {
		return new NodeId(value);
	}
	
}
