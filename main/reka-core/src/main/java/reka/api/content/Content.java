package reka.api.content;

import static reka.util.Util.unsupported;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Hashable;
import reka.api.JsonProvider;
import reka.api.content.types.BinaryContentConverter;
import reka.api.content.types.BooleanContent;
import reka.api.content.types.DoubleContent;
import reka.api.content.types.IntegerContent;
import reka.api.content.types.LongContent;
import reka.api.content.types.NullContent;
import reka.api.content.types.UTF8Content;
import reka.core.data.ObjBuilder;
import reka.util.Util;

public interface Content extends Hashable, JsonProvider {
	
	static final Encoder BASE64_ENCODER = Base64.getEncoder();
	static final Decoder BASE64_DECODER = Base64.getDecoder();
	
	public static final String CUSTOM_TYPE = "@type";
	
	static final Logger logger = LoggerFactory.getLogger("content");
	
	static final BinaryContentConverter BINARY_CONVERTER = new BinaryContentConverter();
	
	public enum Type {
		
		INTEGER(1) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new IntegerContent(in.readInt());
			}
			
		},
		
		DOUBLE(2) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new DoubleContent(in.readDouble());
			}
			
		},
		
		LONG(3) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new LongContent(in.readLong());
			}
			
		},
		
		UTF8(4) {

			@Override
			public Content in(DataInput in) throws IOException {
				byte[] bytes = new byte[in.readInt()];
				in.readFully(bytes);
				return new UTF8Content(new String(bytes, StandardCharsets.UTF_8));
			}
			
		},
		
		NULL(5) {

			@Override
			public Content in(DataInput in) throws IOException {
				return NullContent.INSTANCE;
			}
			
		},
		
		BOOLEAN(6) {

			@Override
			public Content in(DataInput in) throws IOException {
				return BooleanContent.of(in.readBoolean());
			}
			
		},
		
		BINARY(7) {

			@Override
			public Content in(DataInput in) throws IOException {
				return BINARY_CONVERTER.in(in);
			}
			
		},
		
		NON_SERIALIZABLE_OBJECT(99) {

            @Override
            public Content in(DataInput in) throws IOException {
                throw Util.unsupported("can't read object from data input");
            }
		    
		}
		

		;
		private final byte val;
		private Type(int val) {
			this.val = (byte) val;
		}
		public byte identifier() {
			return val;
		}
		public static Type fromIdentifier(byte contentType) {
			switch (contentType) {
			case 1: return INTEGER;
			case 2: return DOUBLE;
			case 3: return LONG;
			case 4: return UTF8;
			case 5: return NULL;
			case 6: return BOOLEAN;
			case 7: return BINARY;
			case 99: return NON_SERIALIZABLE_OBJECT;
			default:
					throw Util.runtime("unknown content type identifier %d\n", contentType);
			}
		}
		public abstract Content in(DataInput in) throws IOException;
	}
	
	/*
	static abstract class BaseContent implements Content {
		
		@Override
		public int asInt() { throw unsupported(); }
		
		@Override
		public String asUTF8() { return toString(); }
		
		@Override
		public double asDouble() { throw unsupported(); }
		
		@Override
		public long asLong() { throw unsupported(); }
		
		@Override
		public byte[] asBytes() { throw unsupported(); }
		
		@Override
		public ByteBuffer asByteBuffer() { throw unsupported(); }
		
		@Override
		public boolean hasFile() {
			return false;
		}	@Override
		public boolean hasValue() {
			return true;
		}
		
		@Override
		public boolean hasByteBuffer() {
			return false;
		}
		
	
		
		@Override
		public File asFile() { throw Util.unsupported(); }
		
		@Override
		public String toString() {
			Object value = value();
			if (value == null) {
				return "null";
			} else {
				return value.toString();
			}
		}
		
		@Override
		public boolean isEmpty() {
			return false;
		}
		
		@Override
		public void writeObj(ObjBuilder obj) {
			obj.writeValue(value());
		}
	}
	*/
	
	public static interface ContentConverter <T extends Content> {
		public T in(DataInput in) throws IOException;
		public void out(T content, DataOutput out) throws IOException;
		public void out(T content, JsonGenerator json) throws IOException;
	}
	
	Object value();
	
	default String asUTF8() { throw unsupported(); }

	default int asInt() { throw unsupported(); }
	default long asLong() { throw unsupported(); }
	default double asDouble() { throw unsupported(); }
	
	default byte[] asBytes() { throw unsupported(); }
	
	default boolean hasFile() { return false; }
	default File asFile() {  throw unsupported(); }
	
	default boolean hasByteBuffer() { return false; }
	default ByteBuffer asByteBuffer() {  throw unsupported(); }
	
	Type type();
	
	void out(DataOutput out) throws IOException;
	
	default void writeObj(ObjBuilder builder) {
		builder.writeValue(value());
	}
	
	default boolean hasValue() { return true; }
	default boolean isEmpty() { return false; }
	
}
