package reka.http.operations;

import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import javax.activation.MimetypesFileTypeMap;

import reka.api.content.Content;
import reka.api.run.Operation;

public abstract class HttpContentUtils {
	
	private static final MimetypesFileTypeMap mimeTypesMap;
	
	static {
		mimeTypesMap = new MimetypesFileTypeMap(HttpContentUtils.class.getResourceAsStream("/mime.types"));
	}
	
	public static Operation httpContent(Content content, String contentType, boolean useEtag) {
		return useEtag ? new HttpContentWithETag(content, contentType) : new HttpContent(content, contentType);
	}

	private static final long PUT_IN_FILE_THRESHOLD = 1024L * 32l; // 32k;
	
	public static class ContentAndType {
		
		private final Content content, type;
		
		private ContentAndType(Content content, Content type) {
			this.content = content;
			this.type = type;
		}
		
		public Content content() {
			return content;
		}
	 
		public Content type() {
			return type;
		}
		
	}
	
	public static ContentAndType convert(Content content, String contentType) {
		
		contentType = mimeType(contentType);
		
		if (!content.hasFile() && !content.hasByteBuffer()) {
			
			byte[] contentBytes = content.asBytes();
			
			if (contentBytes.length > PUT_IN_FILE_THRESHOLD) {
				try {
					File f = Files.createTempFile("reka.", "httpcontent").toFile();
					f.deleteOnExit();
					Files.write(f.toPath(), contentBytes);
					content = binary(contentType, f);
				} catch (IOException e) {
					throw unchecked(e);
				}
			} else {
				ByteBuffer buf = ByteBuffer.allocateDirect(contentBytes.length).put(contentBytes);
				buf.flip();
				content = binary(contentType, buf.asReadOnlyBuffer());
			}
		}
		
		return new ContentAndType(content, utf8(contentType));
		
	}

	private static String mimeType(String val) {
		return val.contains("/") ? val : mimeTypesMap.getContentType("fn." + val);
	}
	
}
