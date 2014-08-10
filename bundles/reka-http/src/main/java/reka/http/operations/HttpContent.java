package reka.http.operations;

import static java.lang.String.format;
import static reka.api.content.Contents.binary;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class HttpContent implements SyncOperation {

	private static final long PUT_IN_FILE_THRESHOLD = 1024L * 512L; // 512k
	
	private final Content content;
	private final Content contentType;
	
	public HttpContent(Content content, Content contentType) {
		
		if (!content.hasFile() && !content.hasByteBuffer()) {
			
			byte[] contentBytes = content.asBytes();
			
			if (contentBytes.length > PUT_IN_FILE_THRESHOLD) {
				try {
					File f = Files.createTempFile("reka.", format(".%s", contentType.asUTF8().replaceAll("[^a-zA-Z0-9_\\-]", "__"))).toFile();
					f.deleteOnExit();
					Files.write(f.toPath(), content.asBytes());
					content = binary(contentType.asUTF8(), f);
				} catch (IOException e) {
					throw unchecked(e);
				}
			} else {
				ByteBuffer buf = ByteBuffer.allocateDirect(contentBytes.length).put(contentBytes);
				buf.flip();
				content = binary(contentType.asUTF8(), buf.asReadOnlyBuffer());
			}
		}
		
		this.content = content;
		this.contentType = contentType;
	}
	
	@Override
	public MutableData call(MutableData data) {
		return data.put(Response.CONTENT, content)
				   .put(Response.Headers.CONTENT_TYPE, contentType);
	}

}
