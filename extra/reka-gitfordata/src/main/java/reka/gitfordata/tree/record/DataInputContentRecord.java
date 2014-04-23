package reka.gitfordata.tree.record;

import static reka.util.Util.unchecked;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import reka.api.content.Content;
import reka.gitfordata.tree.record.builders.ContentRecordBuilder;

public final class DataInputContentRecord implements ContentRecord {
	
	public static DataInputContentRecord in(DataInput in) {
		try {
			byte contentType = in.readByte();
			return new DataInputContentRecord(contentType, in);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public static ContentRecord fromContentTypeAndData(byte contentType, DataInput in) {
		return new DataInputContentRecord(contentType, in);
	}
	
	private DataInputContentRecord(byte contentType, DataInput in) {
		this.contentType = contentType;
		this.in = in;
	}
	
	public static ContentRecordBuilder newBuilder(Content content) {
		return new ContentRecordBuilder(content);
	}

	private final byte contentType;
	private final DataInput in;
	
	private Content content;
	
	@Override
	public void out(DataOutput out) {
		try {
			out.writeInt(contentType);
			toContent().out(out);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	@Override
	public Content.Type contentType() {
		return Content.Type.fromIdentifier(contentType);
	}
	
	@Override
	public Content toContent() {
		if (content == null) {
			try {
				content = Content.Type.fromIdentifier(contentType).in(in);
			} catch (IOException e) {
				throw unchecked(e);
			}
		}
		return content;
	}

	@Override
	public Type recordType() {
		return Type.CONTENT;
	}

}
