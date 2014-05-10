package reka.config.parser2;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.FileSource;
import reka.config.Source;
import reka.config.StringSource;
import reka.config.parser2.states.BodyState;

import com.google.common.collect.ImmutableList;

public class Parser {

	public static void main(String[] args) {
		Source a = StringSource.from("boo yeah\nanother item for me\n\n\n\noh yeah\nkeyfordoc first val <<-\ndoc content\nI can have this in it ♥\n ---\n");
		Source b = FileSource.from(new File("src/test/resources/source-test.conf"));
		log.info("--------------------------- parse a\n\n");
		parse(a);
		log.info("\n\n");

		log.info("--------------------------- parse b\n\n");
		parse(b);
		log.info("\n\n");
	}

	public static class KeyOnlyItem {
		
		private final KeyVal key;
		
		public KeyOnlyItem(KeyVal key) {
			this.key = key;
		}
		
		public KeyVal key() {
			return key;
		}
		
	}
	
	public static class KeyAndValueItem {
		
		private final KeyVal key;
		private final ValueVal value;
		
		public KeyAndValueItem(KeyVal key, ValueVal value) {
			this.key = key;
			this.value = value;
		}
		
		public KeyVal key() {
			return key;
		}
		
		public ValueVal value() {
			return value;
		}
		
		@Override
		public String toString() {
			return format("%s(key='%s', val='%s')", getClass().getSimpleName(), key.value(), value.value());
		}
		
	}
	
	public static abstract class StringVal {
		
		private final String value;

		public StringVal(String value) {
			this.value = value;
		}
		
		public String value() {
			return value;
		}
		
		@Override
		public String toString() {
			return format("%s('%s')", getClass().getSimpleName(), value);
		}
		
	}
	
	public static class BodyVal {
		
		private final List<KeyAndValueItem> items;
		
		public BodyVal(List<KeyAndValueItem> items) {
			this.items = ImmutableList.copyOf(items);
		}
		
		public List<KeyAndValueItem> items() {
			return items;
		}
		
		@Override
		public String toString() {
			return format("%s(%s)", getClass().getSimpleName(), items.stream().map(Object::toString).collect(joining(", ")));
		}
		
	}
	
	public static class KeyVal extends StringVal {

		public KeyVal(String value) {
			super(value);
		}

	}

	public static class DocVal extends StringVal {

		public DocVal(String value) {
			super(value);
		}

	}

	public static class ValueVal extends StringVal {

		public ValueVal(String value) {
			super(value);
		}

	}
	
	static final Logger log = LoggerFactory.getLogger(Parser.class);
	
	public static void parse(Source source) {
		
		ParseContext ctx = new ParseContext(source, new BodyState());
		ctx.start();
		log.info("got {} things:", ctx.toplevelEmissions.size());
		for (Entry<String, Object> e : ctx.toplevelEmissions.entrySet()) {
			log.info(" - {} -> {}", e.getKey(), e.getValue());
		}
	}

}
