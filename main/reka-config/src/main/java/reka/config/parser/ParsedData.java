package reka.config.parser;

import java.io.File;

interface ParsedData {
	String type();
	byte[] content();
	String location();
	boolean isFile();
	File file();
}
