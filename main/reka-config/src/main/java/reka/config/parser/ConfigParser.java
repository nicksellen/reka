package reka.config.parser;

import java.io.File;

import reka.config.FileSource;
import reka.config.NavigableConfig;
import reka.config.Source;
import reka.config.StringSource;
import reka.config.parser2.Parser2;

public class ConfigParser {

	public static NavigableConfig fromFile(File file) {
		return fromSource(FileSource.from(file));
	}
	
	public static NavigableConfig fromString(String content) {
		return fromSource(StringSource.from(content));
	}
	
	public static NavigableConfig fromSource(Source source) {
		return Parser2.parse(source);
	    //return ParsedItem.from(source).toConfig();
	}
	
}
