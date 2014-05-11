package reka.config.parser2;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser2.states.BodyState;

import com.google.common.collect.ImmutableList;

public class Parser2 {

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
	
	public static abstract class ByteVal {
		
		private final byte[] value;

		public ByteVal(byte[] value) {
			this.value = value;
		}
		
		public byte[] value() {
			return value;
		}
		
		@Override
		public String toString() {
			return format("%s('%s')", getClass().getSimpleName(), value);
		}
		
	}
	
	public static class BodyVal {
		
		private final List<Config> configs;
		
		public BodyVal(List<Config> configs) {
			this.configs = ImmutableList.copyOf(configs);
		}
		
		public List<Config> configs() {
			return configs;
		}
		
	}
	
	public static class KeyVal extends StringVal {

		public KeyVal(String value) {
			super(value);
		}

	}

	public static class DocVal extends ByteVal {
		
		private final String contentType;

		public DocVal(String contentType, byte[] value) {
			super(value);
			this.contentType = contentType;
		}
		
		public String contentType() {
			return contentType;
		}

	}

	public static class ValueVal extends StringVal {

		public ValueVal(String value) {
			super(value);
		}

	}
	
	static final Logger log = LoggerFactory.getLogger(Parser2.class);
	
	public static ConfigBody parse(Source source) {
		BodyState root = new BodyState();
		new ParseContext(source, root).run();
		return ConfigBody.of(source, root.configs());
	}

}
