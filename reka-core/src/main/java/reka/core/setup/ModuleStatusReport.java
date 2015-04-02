package reka.core.setup;

import reka.api.data.Data;

public class ModuleStatusReport {

	private final String name;
	private final String alias;
	private final String version;
	private final boolean up;
	private final Data data;
	
	public ModuleStatusReport(String name, String alias, String version, boolean up, Data data) {
		this.name = name;
		this.alias = alias;
		this.version = version;
		this.up = up;
		this.data = data;
	}
	
	public String name() {
		return name;
	}
	
	public String alias() {
		return alias;
	}
	
	public String version() {
		return version;
	}
	
	public boolean up() {
		return up;
	}
	
	public Data data() {
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (up ? 1231 : 1237);
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModuleStatusReport other = (ModuleStatusReport) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (up != other.up)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
	
	
	
}