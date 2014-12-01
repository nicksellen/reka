package reka.net.http.operations;

import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.sha1hex;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.MimetypesFileTypeMap;

import reka.api.content.Content;
import reka.api.run.Operation;

public abstract class HttpContentUtils {
	
	private static final MimetypesFileTypeMap mimeTypesMap;
	
	static {
		mimeTypesMap = new MimetypesFileTypeMap(HttpContentUtils.class.getResourceAsStream("/mime.types"));
	}
	
	public static Operation httpContent(Path tmpdir, Content content, String contentType, boolean useEtag) {
		return useEtag ? new HttpContentWithETag(tmpdir, content, contentType) : new HttpContent(tmpdir, content, contentType);
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
	
	public static ContentAndType convert(java.nio.file.Path basedir, Content content, String contentType) {
		
		contentType = mimeType(contentType);
		
		if (!content.hasFile() && !content.hasByteBuffer()) {
			
			byte[] contentBytes = content.asBytes();
			
			if (contentBytes.length > PUT_IN_FILE_THRESHOLD) {
				try {
					String hex = sha1hex(contentBytes);
					Path httpfile = basedir.resolve("http." + hex);
					if (!Files.exists(httpfile)) Files.write(httpfile, contentBytes);
					content = binary(contentType, httpfile.toFile());
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
