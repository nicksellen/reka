package reka.net.http.server;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static reka.api.Path.dots;
import static reka.data.content.Contents.integer;
import static reka.data.content.Contents.utf8;
import static reka.util.Util.rootExceptionMessage;
import static reka.util.Util.unwrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Subscriber;

public class ChannelHandlerContextDataSubscriber implements Subscriber {

	public static final Path CLOSE_CHANNEL = dots("options.close");
	private static final Logger log = LoggerFactory.getLogger(ChannelHandlerContextDataSubscriber.class);

	private final long started = System.nanoTime();
	private final ChannelHandlerContext context;
	
	ChannelHandlerContextDataSubscriber(ChannelHandlerContext context) {
		this.context = context;
	}
	
	@Override
	public void ok(MutableData data) {
		ChannelFuture writeFuture = context.writeAndFlush(data);
		writeFuture.addListener(new LogHttp(data));
		if (data.existsAt(CLOSE_CHANNEL)) {
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void halted() {
		Data data = MutableMemoryData.create().put(Response.STATUS, integer(404));
		context.writeAndFlush(data).addListener(new LogHttp(data)).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void error(Data incomingData, Throwable error) {
		MutableData data = MutableMemoryData.from(incomingData);
		String acceptHeader = data.getString(Request.Headers.ACCEPT).orElse("");
		if (acceptsHtml(acceptHeader)) {
			htmlErrorMessage(data, error);
		} else if (acceptsJson(acceptHeader)) {
			jsonErrorMessage(data, error);
		} else {
			textErrorMessage(data, error);
		}
		context.writeAndFlush(data).addListener(new LogHttp(data)).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static boolean acceptsHtml(String acceptHeader) {
		return acceptHeader.contains("/html");
	}
	
	private static boolean acceptsJson(String acceptHeader) {
		return acceptHeader.contains("/json");
	}
	
	private static String stacktrace(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
	
	private static void textErrorMessage(MutableData data, Throwable t) {
		data.putString(Response.CONTENT, rootExceptionMessage(unwrap(t)))
			.putString(Response.Headers.CONTENT_TYPE, "text/plain")
			.putInt(Response.STATUS, 500);
	}
	
	private static void jsonErrorMessage(MutableData data, Throwable t) {
		t = unwrap(t);
		data.put(Response.CONTENT, utf8(data.toJson()))
			.putString(Response.CONTENT.add("message"), rootExceptionMessage(t))
			.putString(Response.Headers.CONTENT_TYPE, "application/json")
			.putInt(Response.STATUS, 500);
	}
	
	private static void htmlErrorMessage(MutableData data, Throwable t) {
		t = unwrap(t);
		data.putString(Response.CONTENT, 
                        "<html><body>" +
                                "<h1>Oh noes!</h1>" +
                                "<h2>" +
                                rootExceptionMessage(t) +
                                "</h2>" +
                                "<h3>Stack trace</h3>" +
                                "<pre>" +
                                stacktrace(t) +
                                "</pre>" +
                                "<h3>Data at time of error</h3>" +
                                "<pre>" +
                                escapeHtml4(data.toPrettyJson()) +
                                "</pre>" +
                                "</body></html>")
			.putString(Response.Headers.CONTENT_TYPE, "text/html")
			.putInt(Response.STATUS, 500);

	}
	
	public class LogHttp implements GenericFutureListener<Future<Void>> {

		private final Data data;
		
		public LogHttp(Data data) {
			this.data = data;
		}
		
		@Override
		public void operationComplete(Future<Void> future) throws Exception {
			long took = (System.nanoTime() - started) / 1000;
			String status = data.getString(Response.STATUS).orElse("-");
			log.info("{} - \"{} {}\" {} {}us", 
					data.getString(Request.HOST).orElse(""), 
					data.getString(Request.METHOD).orElse(""), 
					data.getString(Request.PATH).orElse(""), 
					status,
					took);
		}
		
		
		
	}
	
}