package reka;

import java.net.URL;

public class BundleMeta {
	
	private final URL url;
	private final String classname;
	private final String name;
	private final String version;

	public BundleMeta(URL url, String classname, String name, String version) {
		this.url = url;
		this.classname = classname;
		this.name = name;
		this.version = version;
	}
	
	public URL url() {
		return url;
	}
	
	public String classname() {
		return classname;
	}
	
	public String name() {
		return name;
	}

	public String version() {
		return version;
	}

}
