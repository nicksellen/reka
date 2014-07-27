package reka.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.net.URL;

import reka.config.parser.ConfigParser;

public class ConfigTestUtil {

	public static NavigableConfig loadconfig(String path) {
		URL r = ConfigTestUtil.class.getResource(path);
		checkNotNull(r, "no config at %s", path);
		return ConfigParser.fromFile(new File(r.getFile()));
	}

}
