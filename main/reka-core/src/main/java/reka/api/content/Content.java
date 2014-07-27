package reka.api.content;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static reka.util.Util.unsupported;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Hashable;
import reka.api.JsonProvider;
import reka.core.data.ObjBuilder;
import reka.util.Util;

import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.io.BaseEncoding;


public interface Content extends Hashable, JsonProvider {
	
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
				return new UTF8Content(new String(bytes, Charsets.UTF_8));
			}
			
		},
		
		NULL(5) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new NullContent();
			}
			
		},
		
		TRUE(6) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new TrueContent();
			}
			
		},
		
		FALSE(7) {

			@Override
			public Content in(DataInput in) throws IOException {
				return new FalseContent();
			}
			
		},
		
		BINARY(8) {

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
			case 6: return TRUE;
			case 7: return FALSE;
			case 8: return BINARY;
			case 99: return NON_SERIALIZABLE_OBJECT;
			default:
					throw Util.runtime("unknown content type identifier %d\n", contentType);
			}
		}
		public abstract Content in(DataInput in) throws IOException;
	}
	
	static abstract class BaseContent implements Content {
		
		@Override
		public int asInt() { throw unsupported(); }
		
		@Override
		public String asUTF8() { throw unsupported(); }
		
		@Override
		public double asDouble() { throw unsupported(); }
		
		@Override
		public long asLong() { throw unsupported(); }
		
		@Override
		public byte[] asBytes() { throw unsupported(); }
		
		@Override
		public ByteBuffer asByteBuffer() { throw unsupported(); }
		
		@SuppressWarnings("unchecked")
        @Override
		public <T> T valueAs(Class<T> klass) {
		    Object val = value();
		    checkArgument(klass.isInstance(val), "valueAs(%s) failed for [%s]", klass, val);
		    return (T) val;
		}
		
		@Override
		public boolean hasFile() {
			return false;
		}
		
		@Override
		public boolean hasByteBuffer() {
			return false;
		}
		
		@Override
		public boolean hasValue() {
			return true;
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
	
	public static class DoubleContent extends BaseContent {

		private final double value;
		
		public DoubleContent(double value) {
			this.value = value;
		}
		
		@Override
		public Type type() {
			return Type.DOUBLE;
		}

		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			json.writeNumber(value);
		}

		@Override
		public void out(DataOutput out) throws IOException {
			out.writeDouble(value);
		}

		@Override
		public double asDouble() {
			return value;
		}
		
		@Override
		public Object value() {
			return value;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putDouble(value);
		}

		@Override
		public String asUTF8() {
			return toString();
		}
		
		@Override
		public String toString() {
			return Double.toString(value);
		}
		
	}
	
	public static class IntegerContent extends BaseContent {

		private final int value;
		
		public IntegerContent(int value) {
			this.value = value;
		}

		@Override
		public void writeJsonTo(JsonGenerator out) throws IOException {
			out.writeNumber(value);
		}

		@Override
		public void out(DataOutput out) throws IOException {
			out.writeInt(value);
		}

		@Override
		public Type type() {
			return Type.INTEGER;
		}
		
		@Override
		public String toString() {
			return Integer.toString(value);
		}
		
		@Override
		public int asInt() {
			return value;
		}

		@Override
		public Object value() {
			return value;
		}
		
		@Override
		public String asUTF8() {
			return toString();
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putInt(value);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IntegerContent other = (IntegerContent) obj;
			if (value != other.value)
				return false;
			return true;
		}
		
	}
	
	public static class LongContent extends BaseContent {

		private final long value;
		
		public LongContent(long value) {
			this.value = value;
		}

		@Override
		public void writeJsonTo(JsonGenerator out) throws IOException {
			out.writeNumber(value);
		}

		@Override
		public void out(DataOutput out) throws IOException {
			out.writeLong(value);
		}

		@Override
		public Type type() {
			return Type.INTEGER;
		}
		
		@Override
		public String toString() {
			return Long.toString(value);
		}
		
		@Override
		public long asLong() {
			return value;
		}

		@Override
		public Object value() {
			return value;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putLong(value);
		}
		
		@Override
		public String asUTF8() {
			return toString();
		}
		
	}
	
	public static class TrueContent extends BaseContent {

		public static final TrueContent INSTANCE = new TrueContent();
		
		private TrueContent() {}

		@Override
		public Type type() {
			return Type.TRUE;
		}

		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			json.writeBoolean(true);
		}

		@Override
		public void out(DataOutput out) throws IOException {
		}

		@Override
		public Object value() {
			return true;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putBoolean(true);
		}
		
	}
	
	public static class FalseContent extends BaseContent {
		
		public static final FalseContent INSTANCE = new FalseContent();
		
		private FalseContent() {}

		@Override
		public Type type() {
			return Type.FALSE;
		}

		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			json.writeBoolean(false);
		}

		@Override
		public void out(DataOutput out) throws IOException {
		}

		@Override
		public Object value() {
			return false;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putBoolean(false);
		}
		
	}
	
	public static class NullContent extends BaseContent {

		public static final NullContent INSTANCE = new NullContent();
		
		private NullContent() {}
		
		@Override
		public Type type() {
			return Type.NULL;
		}

		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			json.writeNull();
		}

		@Override
		public void out(DataOutput out) throws IOException {
		}

		@Override
		public Object value() {
			return null;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putByte((byte)0);
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
		
	}
	
	public static class UTF8Content extends BaseContent {
		
		private final String value;
		
		public UTF8Content(String value) {
			this.value = value;
		}

		@Override
		public void writeJsonTo(JsonGenerator out) throws IOException {
			out.writeString(value);
		}

		@Override
		public void out(DataOutput out) throws IOException {
			byte[] bytes = value.getBytes(Charsets.UTF_8);
			out.writeInt(bytes.length);
			out.write(bytes);
		}

		@Override
		public Type type() {
			return Type.UTF8;
		}
		
		@Override
		public String toString() {
			return value;
		}
		
		@Override
		public String asUTF8() {
			return value;
		}

		@Override
		public Object value() {
			return value;
		}

		@Override
		public Hasher hash(Hasher hasher) {
			return hasher.putString(value);
		}

		@Override
		public boolean isEmpty() {
			return value.isEmpty();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UTF8Content other = (UTF8Content) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}
	public static interface ContentConverter <T extends Content> {
		public T in(DataInput in) throws IOException;
		public void out(T content, DataOutput out) throws IOException;
		public void out(T content, JsonGenerator json) throws IOException;
	}
	
	public static class BinaryContentConverter implements ContentConverter<BinaryContent> {

		@Override
		public BinaryContent in(DataInput in) throws IOException {
			String contentType = in.readUTF();
			if (contentType.isEmpty()) {
				contentType = null;
			}
			BinaryContent.Encoding encoding = BinaryContent.Encoding.valueOf(in.readUTF().toUpperCase());
			long size = in.readLong();
			byte[] bytes = new byte[(int) size];
			in.readFully(bytes);
			return new ByteArrayBinaryContent(contentType, encoding, bytes);
		}

		@Override
		public void out(BinaryContent content, DataOutput out) throws IOException  {
			if (content.contentType == null) {
				out.writeUTF("");
			} else {
				out.writeUTF(content.contentType);
			}
			out.writeUTF(content.encoding().toString().toLowerCase());
			out.writeLong(content.size());
			// TODO: use a streaming technique if possible
			out.write(content.bytes());
		}

		@Override
		public void out(BinaryContent content, JsonGenerator json) throws IOException {
			json.writeStartObject();
			json.writeStringField(Content.CUSTOM_TYPE, BinaryContent.JSON_TYPE);
			json.writeStringField("content-encoding", BinaryContent.Encoding.BASE64.toString().toLowerCase());
			if (content.contentType != null) {
				json.writeStringField("content-type", content.contentType);
			}
			json.writeFieldName("content");
			switch (content.encoding()) {
			case NONE: 
				json.writeString(BaseEncoding.base64().encode(content.bytes()));
				break;
			case BASE64: 
				// it's already base64, don't need to do anything
				json.writeString(new String(content.bytes(), Charsets.UTF_8));
				break;
			default:
				throw Util.runtime("don't know how to write %s encoded content to json", content.encoding());
			}
			json.writeEndObject();
		}
		
	}
	
	public static class ByteBufferBinaryContent extends BinaryContent {
		private final ByteBuffer buffer;
		
		public ByteBufferBinaryContent(String contentType, Encoding encoding, ByteBuffer buffer) {
			super(contentType, encoding);
			this.buffer = buffer;
		}
		
		@Override
		public boolean hasByteBuffer() {
			return true;
		}
		
		@Override
		public ByteBuffer asByteBuffer() {
			return buffer;
		}

		@Override
		protected byte[] bytes() {
			buffer.mark();
			try {
				byte[] result = new byte[buffer.remaining()];
				buffer.get(result);
				return result;
			} finally {
				buffer.reset();
			}
		}

		@Override
		protected long size() {
			return buffer.remaining();
		}
		
		@Override
		public Object value() {
			return buffer;
		}
		
	}
	
	public static class FileBinaryContent extends BinaryContent {
		private final File file;
		
		public FileBinaryContent(String contentType, Encoding encoding, File file) {
			super(contentType, encoding);
			this.file = file;
		}
		
		@Override
		public boolean hasFile() {
			return true;
		}
		
		@Override
		public File asFile() {
			return file;
		}

		@Override
		protected byte[] bytes()  {
			try {
				return Files.readAllBytes(file.toPath());
			} catch (IOException e) {
				throw Util.unchecked(e);
			}
		}

		@Override
		protected long size() {
			return file.length();
		}
	}
	
	public static class ByteArrayBinaryContent extends BinaryContent {
		
		private final byte[] bytes;
		
		public ByteArrayBinaryContent(String contentType, Encoding encoding, byte[] bytes) {
			super(contentType, encoding);
			this.bytes = bytes;
		}

		@Override
		protected byte[] bytes()  {
			return bytes;
		}

		@Override
		protected long size() {
			return bytes.length;
		}
		
	}
	
	public abstract static class BinaryContent extends BaseContent {
		
		private static BaseEncoding BASE64_ENCODING = BaseEncoding.base64();
		
		public static enum Encoding { NONE, BASE64 };
		
		public static final String JSON_TYPE = "binary/1.0";
		
		protected final String contentType;
		protected final Encoding encoding; // what kind of bytes do we have?
		
		protected BinaryContent(String contentType, Encoding encoding) {
			if (contentType == null) {
				Util.printStackTrace();
			}
			checkNotNull(contentType, "must include content type");
			this.contentType = contentType;
			this.encoding = encoding;
		}
		
		@Override
		public void writeObj(ObjBuilder obj) {
			if (contentType != null && contentType.startsWith("text/")) {
				obj.writeValue(new String(as(Encoding.NONE).bytes(), Charsets.UTF_8));
			} else {
				obj.writeValue(as(Encoding.NONE).bytes()); // ? this might not really want a byte array...
			}
		}
		
		@Override
		public Object value() {
			logger.warn("not sure why we are using binarycontent.value()");
			Util.printStackTrace();
			return as(Encoding.NONE).bytes();
		}

		@Override
		public Type type() {
			return Type.BINARY;
		}

		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			BINARY_CONVERTER.out(this, json);
		}

		@Override
		public void out(DataOutput out) throws IOException {
			BINARY_CONVERTER.out(this, out);
		}

		@Override
		public Hasher hash(Hasher hasher) {
			if (contentType != null) {
				hasher.putString(contentType);
			}
			return hasher.putLong(size()).putBytes(bytes());
		}
		
		@Override
		public byte[] asBytes() {
			return bytes();
		}
		
		public String asDataURI() {
			checkState(contentType != null, "can't make a DataURI without the content/type");
			return format("data:%s;base64,%s", contentType, base64String());
		}
		
		public BinaryContent as(Encoding requiredEncoding) {
			if (requiredEncoding == encoding) return this;
			return new ByteArrayBinaryContent(contentType, requiredEncoding, encoded(requiredEncoding));
		}
		
		private byte[] encoded(Encoding requiredEncoding) {
			if (requiredEncoding == encoding) {
				return bytes();
			} else if (encoding == Encoding.BASE64 && requiredEncoding == Encoding.NONE) {
				return BASE64_ENCODING.decode(new String(bytes(), Charsets.UTF_8));
			} else if (encoding == Encoding.NONE && requiredEncoding == Encoding.BASE64) {
				return base64String().getBytes(Charsets.UTF_8);
			} else {
				throw Util.runtime("don't know how to turn [%s] -> [%s]", encoding, requiredEncoding);
			}
		}
		
		public String base64String() {
			if (encoding == Encoding.BASE64) {
				return new String(bytes(), Charsets.UTF_8);
			} else {
				return BASE64_ENCODING.encode(bytes());
			}
		}
		
		public byte[] base64() {
			return encoded(Encoding.BASE64);
		}
		
		public byte[] decoded() {
			return encoded(Encoding.NONE);
		}
		
		public Encoding encoding() {
			return encoding;
		}
		
		public String contentType() {
			return contentType;
		}
		
		protected abstract byte[] bytes();
		protected abstract long size();

		@Override
		public boolean isEmpty() {
			return size() == 0;
		}
		
		@Override
		public String toString() {
			if (contentType != null) {
				return format("BinaryContent(%s, %d bytes)", contentType, size());
			} else {
				return format("BinaryContent(%d bytes)", size());
			}
		}
		
		@Override
		public String asUTF8() {
			return new String(bytes(), Charsets.UTF_8);
		}
		
	}
	
	public static class NonSerializeableObject extends BaseContent {
	    
	    private final Object object;
	    
	    public NonSerializeableObject(Object object) {
	        this.object = object;
	    }

        @Override
        public Object value() {
            return object;
        }

        @Override
        public Type type() {
            return Type.NON_SERIALIZABLE_OBJECT;
        }

        @Override
        public void writeJsonTo(JsonGenerator json) throws IOException {
        	json.writeStartObject();
        	json.writeStringField("class", object.getClass().getName());
        	json.writeStringField("toString", object.toString());
        	json.writeEndObject();
        }

        @Override
        public void out(DataOutput out) throws IOException {
            throw Util.unsupported();
        }

        @Override
        public Hasher hash(Hasher hasher) {
            throw Util.unsupported();
        }
        
	}
	
	public Object value();
	public <T> T valueAs(Class<T> klass);
	public int asInt();
	public String asUTF8();
	public double asDouble();
	public long asLong();
	public byte[] asBytes();
	
	public boolean hasFile();
	public File asFile();
	
	public boolean hasByteBuffer();
	public ByteBuffer asByteBuffer();
	
	public Type type();
	
	public void out(DataOutput out) throws IOException;
	public void writeObj(ObjBuilder builder);
	
	public boolean hasValue();
	public boolean isEmpty();
}
