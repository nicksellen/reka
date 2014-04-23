package reka.http.operations;

import static java.lang.String.format;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

public class HttpContent implements SyncOperation {

	private static final long PUT_IN_FILE_THRESHOLD = 1024L * 512L; // 512k

	private static final HashFunction sha1 =  Hashing.sha1();
	private final static BaseEncoding hex = BaseEncoding.base16();
	
	private final Content content;
	private final Content contentType;
	private final String etag;
	
	private static final Content EMPTY = utf8("");
	private static final Content NOT_MODIFIED = integer(304);
	
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
		Hasher hasher = sha1.newHasher();
		content.hash(hasher);
		hasher.putByte((byte)0);
		contentType.hash(hasher);
		etag = hex.encode(hasher.hash().asBytes());
	}
	
	@Override
	public MutableData call(MutableData data) {
		if (etag.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
			return data
				.put(Response.CONTENT, EMPTY)
				.put(Response.STATUS, NOT_MODIFIED);
		} else {
			return data
				.put(Response.CONTENT, content)
				.put(Response.Headers.CONTENT_TYPE, contentType)
				.put(Response.Headers.ETAG, utf8(etag));
		}
	}

}
