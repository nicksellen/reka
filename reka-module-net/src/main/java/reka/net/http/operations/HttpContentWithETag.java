package reka.net.http.operations;

import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;
import static reka.util.Util.hex;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import reka.data.MutableData;
import reka.data.content.Content;
import reka.data.content.Contents;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.net.http.operations.HttpContentUtils.ContentAndType;
import reka.util.Path.Request;
import reka.util.Path.Response;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class HttpContentWithETag implements Operation {
	
	private static final HashFunction sha1 =  Hashing.sha1();
	
	private final Content content, contentType;
	
	private final String etagValue;
	private final Content etag;
	
	private static final Content EMPTY = Contents.nullValue();	
	private static final Content NOT_MODIFIED = integer(304);
	
	protected HttpContentWithETag(Path tmpdir, Content content, String contentType) {
		ContentAndType vals = HttpContentUtils.convert(tmpdir, content, contentType);
		this.content = vals.content();
		this.contentType = vals.type();
		Hasher hasher = sha1.newHasher();
		content.hash(hasher);
		hasher.putString(contentType, StandardCharsets.UTF_8);
		etagValue = hex(hasher.hash().asBytes());
		etag = utf8(etagValue);
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		if (data.existsAt(Request.Headers.IF_NONE_MATCH) && etagValue.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
			data.put(Response.CONTENT, EMPTY)
				.put(Response.STATUS, NOT_MODIFIED);
		} else {
			data.put(Response.CONTENT, content)
				.put(Response.Headers.CONTENT_TYPE, contentType)
				.put(Response.Headers.ETAG, etag);
		}
	}

}
