package reka.http.operations;

import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

public class HttpContentWithETag extends BaseHttpContent implements AsyncOperation {

	private static final HashFunction sha1 =  Hashing.sha1();
	private final static BaseEncoding hex = BaseEncoding.base16();
	
	private final String etagValue;
	private final Content etag;
	
	private static final Content EMPTY = utf8("");
	private static final Content NOT_MODIFIED = integer(304);
	
	public HttpContentWithETag(Content content, Content contentType) {
		super(content, contentType);
		Hasher hasher = sha1.newHasher();
		content.hash(hasher);
		contentType.hash(hasher);
		etagValue = hex.encode(hasher.hash().asBytes());
		etag = utf8(etagValue);
	}

	@Override
	public void call(MutableData data, OperationResult ctx) {
		if (data.existsAt(Request.Headers.IF_NONE_MATCH) && etagValue.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
			data.put(Response.CONTENT, EMPTY)
				.put(Response.STATUS, NOT_MODIFIED);
		} else {
			data.put(Response.CONTENT, content)
				.put(Response.Headers.CONTENT_TYPE, contentType)
				.put(Response.Headers.ETAG, etag);
		}
		ctx.done();
	}

}
