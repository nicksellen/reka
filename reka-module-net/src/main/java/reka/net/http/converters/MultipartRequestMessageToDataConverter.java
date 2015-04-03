package reka.net.http.converters;

import static reka.data.content.Contents.binary;
import static reka.data.content.Contents.utf8;
import static reka.util.Path.CONTENT;
import static reka.util.Path.dots;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;

public class MultipartRequestMessageToDataConverter implements HttpMessageToDataConverter {

	private static final Logger log = LoggerFactory.getLogger(MultipartRequestMessageToDataConverter.class);
	
	@Override
	public void processData(FullHttpMessage message, MutableData out, String contentType) throws Exception {
		MutableData requestData = out.createMapAt(CONTENT);
		HttpPostRequestDecoder post = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), (FullHttpRequest)message);
		int uploadCount = 0;
		for (InterfaceHttpData postdata : post.getBodyHttpDatas()) {
			log.debug("{} -> {}", postdata.getName(), postdata.getHttpDataType());
			switch (postdata.getHttpDataType()) {
			case FileUpload:
				FileUpload upload = (FileUpload) postdata;
				
				MutableData postItem = MutableMemoryData.create();
				postItem
					//.put(dots("name"), utf8(upload.getName()))
					.putString(dots("filename"), upload.getFilename())
					.put(dots("data"), binary(upload.getContentType(), upload.getByteBuf().retain().nioBuffer().asReadOnlyBuffer()));
										
				requestData.put(dots(postdata.getName()), postItem);
				uploadCount++;
				break;
			case Attribute:
				 Attribute attribute = (Attribute) postdata;
				 requestData.put(dots(attribute.getName()), utf8(attribute.getValue()));
				 uploadCount++;
				break;
			default:
				break;
			}
		}
		if (uploadCount == 0) {
			log.debug("ah no uploads, %d readable bytes..", message.content().readableBytes());
		} else {
			log.debug("{} upload(s)", uploadCount);
			requestData.forEachContent((path, content) -> {
				log.debug("  {} -> {}", path.dots(), content);
			});
		}
	}
	
}