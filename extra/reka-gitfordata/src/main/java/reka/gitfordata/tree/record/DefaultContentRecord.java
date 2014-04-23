package reka.gitfordata.tree.record;

import static reka.util.Util.unchecked;

import java.io.DataOutput;
import java.io.IOException;

import reka.api.content.Content;

public class DefaultContentRecord implements ContentRecord {
	
	private final Content content;
	
	public static ContentRecord fromContent(Content content) {
		return new DefaultContentRecord(content);
	}
	
	private DefaultContentRecord(Content content) {
		this.content = content;
	}

	@Override
	public void out(DataOutput out) {
		try {
			out.writeByte(content.type().identifier());
			content.out(out);
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	@Override
	public Content.Type contentType() {
		return content.type();
	}

	@Override
	public Content toContent() {
		return content;
	}

	@Override
	public Type recordType() {
		return Type.CONTENT;
	}

}
