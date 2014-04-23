package reka.config.parser;

import java.io.File;

interface ParsedDocument {
	String type();
	byte[] content();
	boolean isFile();
	File file();
}