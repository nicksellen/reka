package reka.http.server;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.Collections.synchronizedList;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unwrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.EverythingSubscriber;
import reka.core.data.memory.MutableMemoryData;

@ChannelHandler.Sharable
public class HttpVhostHandler extends SimpleChannelInboundHandler<MutableData> {
	
	private static final Path CLOSE_CHANNEL = Path.dots("options.close");
	
	private static final Logger log = LoggerFactory.getLogger(HttpVhostHandler.class);
	
	private final ConcurrentMap<String,List<Consumer<Flow>>> frozen = new ConcurrentHashMap<>();
	
	private final ConcurrentMap<String,Flow> flows = new ConcurrentHashMap<>();
	
	public boolean isEmpty() {
		return flows.isEmpty();
	}
	
	public HttpVhostHandler add(String host, Flow flow) {
		flows.put(host, flow);
		List<Consumer<Flow>> waiting = frozen.remove(host);
		if (waiting != null) {
			synchronized (waiting) {
				log.info("unfreezing {} connections for host {}", waiting.size(), host);
				for (Consumer<Flow> c : waiting) {
					c.accept(flow);
				}
			}
		}
		return this;
	}

	public boolean remove(String host) {
		if (flows.remove(host) != null) {
			log.info("removed host {}", host);
			frozen.remove(host);
			return true;
		}
		return false;
	}

	public void freeze(String host) {
		frozen.putIfAbsent(host, synchronizedList(new ArrayList<>()));
	}
	
	private static class ChannelHandlerContextDataSubscriber implements EverythingSubscriber {

		private final ChannelHandlerContext context;
		
		ChannelHandlerContextDataSubscriber(ChannelHandlerContext context) {
			this.context = context;
		}
		
		@Override
		public void ok(MutableData data) {
			ChannelFuture writeFuture = context.writeAndFlush(data);
			if (data.existsAt(CLOSE_CHANNEL)) {
				writeFuture.addListener(ChannelFutureListener.CLOSE);
			}
		}

		@Override
		public void halted() {

            MutableData data = MutableMemoryData.create()
				.put(Path.Response.CONTENT, utf8("uh, oh it got halted :("))
				.put(Path.Response.Headers.CONTENT_TYPE, utf8("text/plain"))
				.put(Path.Response.STATUS, integer(500));
			
			ChannelFuture writeFuture = context.writeAndFlush(data);
			
			if (data.existsAt(CLOSE_CHANNEL)) {
				writeFuture.addListener(ChannelFutureListener.CLOSE);
			}
			
		}

		@Override
		public void error(Data data, Throwable incoming) {
			incoming.printStackTrace();
			Throwable t = unwrap(incoming);
			StringWriter stackTrace = new StringWriter();
			t.printStackTrace(new PrintWriter(stackTrace));
			
			MutableData responseData = MutableMemoryData.create()
					.putString(Path.Response.CONTENT, 
                            "<html><body>" +
                                    "<h1>Oh noes!</h1>" +
                                    "<h2>" +
                                    rootExceptionMessage(t) +
                                    "</h2>" +
                                    "<h3>Stack trace</h3>" +
                                    "<pre>" +
                                    stackTrace +
                                    "</pre>" +
                                    "<h3>Data at time of error</h3>" +
                                    "<pre>" +
                                    StringEscapeUtils.escapeHtml4(data.toPrettyJson()) +
                                    "</pre>" +
                                    "</body></html>")
					.putString(Path.Response.Headers.CONTENT_TYPE, "text/html")
					.putInt(Path.Response.STATUS, 500);
			
			ChannelFuture writeFuture = context.writeAndFlush(responseData);
			if (responseData.existsAt(CLOSE_CHANNEL)) {
				writeFuture.addListener(ChannelFutureListener.CLOSE);
			}
		}
		
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext context, MutableData data) {

		String host = data.getString(Path.Request.HOST).orElse("localhost");
		
		if (frozen.containsKey(host)) {
			List<Consumer<Flow>> waiting = frozen.get(host);
			synchronized (waiting) {
				// now we have the waiting list locked, check we are still frozen
				if (frozen.containsKey(host)) {
					waiting.add(flow -> {
						executeFlow(context, flow, data, host);
					});
					return;	
				}
			}
		}
		
		Flow flow = flows.get(host);
		
		if (flow == null) {
			noFlowFound(context, data, host);
			return;
		}
		
		executeFlow(context, flow, data, host);
	}
	
	private void noFlowFound(ChannelHandlerContext context, Data data, String host) {
		log.debug("no flow registered for host {}", host);
		context.close();
	}
	
	private void executeFlow(ChannelHandlerContext context, Flow flow, MutableData data, String host) {
		log.info("[{}] {} {}", flow.fullName(), data.getString(Request.METHOD).orElse(""), data.getString(Request.PATH).orElse(""));
		flow.run(listeningDecorator(context.executor()), data, new ChannelHandlerContextDataSubscriber(context));
	}

	private static String rootExceptionMessage(Throwable t) {
		return allExceptionMessages(t).iterator().next();
	}
	
	private static Collection<String> allExceptionMessages(Throwable t) {
		List<String> result = new ArrayList<>();
		
		while (t != null) {
			if (t.getMessage() != null) {
				result.add(t.getMessage());
			}
			t = t.getCause();
		}
		
		Collections.reverse(result);
		
		if (result.isEmpty()) {
			result.add("unknown error");
		}
		
		return result;
	}
	
}
