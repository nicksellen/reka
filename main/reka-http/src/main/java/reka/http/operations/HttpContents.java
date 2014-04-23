package reka.http.operations;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.content.Content;
import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;
import reka.api.run.RouteCollector;
import reka.api.run.RoutingOperation;
import reka.api.run.SyncOperation;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

public abstract class HttpContents implements FlowOperation {
	
	public static class Routed extends HttpContents implements RoutingOperation {
		
		public Routed(Map<String, ContentItem> contents) {
			super(contents);
		}

		@Override
		public MutableData call(MutableData data, RouteCollector router) {
			data = process(data);

			if (data.existsAt(Response.CONTENT)) {
				router.routeTo(FOUND);
			} else {
				router.routeTo(PASSTHROUGH);
			}
			
			return data;
		}
		
	}
	
	public static class Basic extends HttpContents implements SyncOperation {
		
		public Basic(Map<String, ContentItem> contents) {
			super(contents);
		}

		@Override
		public MutableData call(MutableData data) {
			return process(data);
		}
		
	}
	
	private final static BaseEncoding HEX_ENCODING = BaseEncoding.base16();
	
	private static String PASSTHROUGH = "pass";
	private static String FOUND = "found";

	private static final Path UNIQUE_PATHS_PATH = path("data", "content", "permalink");
	
	private static final HashFunction hash =  Hashing.sha1();
	private static final String farFetchedExpires = "Wed, 19 Dec 2100 02:56:04 GMT";
	private static final String neverExpireCacheControl = "max-age=9999999";
	
	private final Map<String, ContentItem> contents;
	private final Map<String, String> pathToEtag;
	
	private final Map<String, String> uniquePathToPath;
	private final Map<String, String> pathToUniquePath;
	
	public HttpContents(Map<String, ContentItem> contents) {
		this.contents = contents;
		Map<String,String> _pathToEtag = new HashMap<>();
		Map<String,String> _uniquePathToPath = new HashMap<>();
		Map<String,String> _pathToUniquePath = new HashMap<>();
		for (Entry<String, ContentItem> entry : contents.entrySet()) {
			String path = entry.getKey();
			
			// hash is made of path + content
			byte[] hashBytes = entry.getValue().hash(hash.newHasher().putString(path)).hash().asBytes();
			String etag = HEX_ENCODING.encode(hashBytes);
			String uniquePath = format("/%s/-%s", etag, path); 
			_pathToEtag.put(path, etag);
			_uniquePathToPath.put(uniquePath, path);
			_pathToUniquePath.put(path, uniquePath);
		}
		
		pathToEtag = ImmutableMap.copyOf(_pathToEtag);
		uniquePathToPath = ImmutableMap.copyOf(_uniquePathToPath);
		pathToUniquePath = ImmutableMap.copyOf(_pathToUniquePath);
		
		/*
		for (Entry<String,String> entry : pathToEtag.entrySet()) {
			logger.info("path -> etag : [{}] -> [{}]", entry.getKey(), entry.getValue());
		}
		*/
	}
	
	protected MutableData process(MutableData data) {
		
		String path = data.getString(Request.PATH).orElse("/");
		
		// this lets things down the chain use these paths...
		for (Entry<String, String> entry : pathToUniquePath.entrySet()) {
			data.put(UNIQUE_PATHS_PATH.add(entry.getKey()), utf8(entry.getValue()));
		}
		
		String pathFromUniquePath = uniquePathToPath.get(path); // if they put in a special unique path
		if (pathFromUniquePath != null) {
			
			// etag path request - this will NEVER change :)
			
			ContentItem content = contents.get(pathFromUniquePath);
			
			data.put(Response.CONTENT, content.content)
				.put(Response.Headers.LINK, utf8(format("<%s>; rel=\"alternate\"", pathFromUniquePath)))
				.put(Response.Headers.EXPIRES, utf8(farFetchedExpires))
				.put(Response.Headers.CACHE_CONTROL, utf8(neverExpireCacheControl))
				.put(Response.Headers.CONTENT_TYPE, content.contentType);
			
		} else {
			
			// normal request
			
			String etag = pathToEtag.get(path);
			if (etag != null) { // if it doesn't have an etag we don't have it
				if (etag.equals(data.getString(Request.Headers.IF_NONE_MATCH).orElse(""))) {
					data.put(Response.STATUS, integer(304))
						.put(Response.Headers.LINK, utf8(format("<%s>; rel=\"alternate\"", pathToUniquePath.get(path))))
						.put(Response.CONTENT, utf8(""));
				} else {
			 		ContentItem content = contents.get(path);
					if (content != null) {
						data.put(Response.CONTENT, content.content)
							.put(Response.Headers.LINK, utf8(format("<%s>; rel=\"alternate\"", pathToUniquePath.get(path))))
							.put(Response.Headers.ETAG, utf8(etag))
							.put(Response.Headers.CACHE_CONTROL, utf8("no-cache")) // use etag only
							.put(Response.Headers.CONTENT_TYPE, content.contentType);
					}
				}
			}
		}
		
		return data;
	}
	
	public static class ContentItem {
		private final Content content;
		private final Content contentType;
		
		private final byte[] hashed;
		
		public ContentItem(byte[] bytes, String contentType) {
			ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).put(bytes);
			buffer.flip();
			this.content = binary(contentType, buffer);
			this.contentType = utf8(contentType);
			
			hashed = Hashing.sha1().hashBytes(bytes).asBytes();
		}
		
		public Hasher hash(Hasher hasher) {
			return hasher.putBytes(hashed).putString(contentType.asUTF8());
		}
	}
	
}
