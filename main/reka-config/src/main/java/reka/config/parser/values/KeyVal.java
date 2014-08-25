package reka.config.parser.values;

import static java.lang.String.format;

import java.util.Iterator;

import reka.config.Config;

import com.google.common.base.Splitter;

public class KeyVal {

	private final String key;
	private final String subkey;

	private final static Splitter s = Splitter.on(":").limit(2);
	
	public static KeyVal parse(String value) {
		Iterator<String> it = s.split(value).iterator();
		String key = it.next();
		String subkey;
		if (it.hasNext()) {
			subkey = it.next();
		} else {
			subkey = null;
		}
		return new KeyVal(key, subkey);
	}
	
	public KeyVal(String key, String subkey) {
		this.key = key;
		this.subkey = subkey;
	}
	
	public KeyVal(String key) {
		this(key, null);
	}
	
	public KeyVal(Config config) {
		this(config.key(), config.subkey());
	}

	public String key() {
		return key;
	}
	
	public String subkey() {
		return subkey;
	}
	
	@Override
	public String toString() {
		if (subkey != null) {
			return format("%s:%s", key, subkey);
		} else {
			return key;
		}
	}
	
}
