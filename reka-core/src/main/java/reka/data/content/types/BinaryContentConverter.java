package reka.data.content.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.codehaus.jackson.JsonGenerator;

import reka.data.content.Content;
import reka.data.content.Content.ContentConverter;
import reka.util.Util;

public class BinaryContentConverter implements ContentConverter<BinaryContent> {

	private final boolean writeContent = false;
	
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
		if (writeContent) {
			json.writeFieldName("content");
			switch (content.encoding()) {
			case NONE: 
				json.writeString(new String(Content.BASE64_ENCODER.encode(content.bytes()), StandardCharsets.UTF_8));
				break;
			case BASE64: 
				// it's already base64, don't need to do anything
				json.writeString(new String(content.bytes(), StandardCharsets.UTF_8));
				break;
			default:
				throw Util.runtime("don't know how to write %s encoded content to json", content.encoding());
			}
		} else {
			json.writeBooleanField("stub", true);
		}
		json.writeEndObject();
	}
	
}