package reka;

public class BundleMeta {
	
	private final String classname;
	private final String version;

	public BundleMeta(String classname, String version) {
		this.classname = classname;
		this.version = version;
	}
	
	public String classname() {
		return classname;
	}

	public String version() {
		return version;
	}

}
