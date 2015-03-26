package reka.net.http.server;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static reka.api.Path.dots;
import static reka.api.content.Contents.integer;
import static reka.util.Util.rootExceptionMessage;
import static reka.util.Util.unwrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;

class ChannelHandlerContextDataSubscriber implements Subscriber {

	public static final Path CLOSE_CHANNEL = dots("options.close");
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final long started = System.nanoTime();
	private final ChannelHandlerContext context;
	
	ChannelHandlerContextDataSubscriber(ChannelHandlerContext context) {
		this.context = context;
	}
	
	@Override
	public void ok(MutableData data) {
		ChannelFuture writeFuture = context.writeAndFlush(data);
		long took = (System.nanoTime() - started) / 1000;
		String status = data.getString(Response.STATUS).orElse("-");
		log.info("{} - \"{} {}\" {}us", 
				data.getString(Request.HOST).orElse(""), 
				data.getString(Request.METHOD).orElse(""), 
				data.getString(Request.PATH).orElse(""), 
				status,
				took);
		if (data.existsAt(CLOSE_CHANNEL)) {
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void halted() {
		context.writeAndFlush(MutableMemoryData.create()
				.put(Response.STATUS, integer(404)))
			.addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void error(Data data, Throwable error) {
		Data msg;
		String acceptHeader = data.getString(Request.Headers.ACCEPT).orElse("");
		if (acceptsHtml(acceptHeader)) {
			msg = htmlErrorMessage(data, error);
		} else if (acceptsJson(acceptHeader)) {
			msg = jsonErrorMessage(data, error);
		} else {
			msg = textErrorMessage(data, error);
		}
		context.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
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
	
	private static Data textErrorMessage(Data data, Throwable t) {
		return MutableMemoryData.create()
				.putString(Response.CONTENT, rootExceptionMessage(unwrap(t)))
				.putString(Response.Headers.CONTENT_TYPE, "text/plain")
				.putInt(Response.STATUS, 500);
	}
	
	private static Data jsonErrorMessage(Data data, Throwable t) {
		t = unwrap(t);
		return MutableMemoryData.create()
					.put(Response.CONTENT.add("data"), data)
					.putString(Response.CONTENT.add("message"), rootExceptionMessage(t))
				.putString(Response.Headers.CONTENT_TYPE, "application/json")
				.putInt(Response.STATUS, 500);
	}
	
	private static Data htmlErrorMessage(Data data, Throwable t) {
		t = unwrap(t);
		return MutableMemoryData.create()
				.putString(Response.CONTENT, 
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
	
}