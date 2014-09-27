package reka.http.operations;

import static reka.api.content.Contents.binary;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import reka.api.content.Content;

public abstract class BaseHttpContent {

	private static final long PUT_IN_FILE_THRESHOLD = 1024L * 32l; // 32k;
	
	protected final Content content;
	protected final Content contentType;
	
	public BaseHttpContent(Content content, Content contentType) {
		
		if (!content.hasFile() && !content.hasByteBuffer()) {
			
			byte[] contentBytes = content.asBytes();
			
			if (contentBytes.length > PUT_IN_FILE_THRESHOLD) {
				try {
					File f = Files.createTempFile("reka.", "httpcontent").toFile();
					f.deleteOnExit();
					Files.write(f.toPath(), contentBytes);
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
	
}
