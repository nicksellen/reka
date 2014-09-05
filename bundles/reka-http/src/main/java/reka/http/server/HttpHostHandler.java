package reka.http.server;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.Collections.synchronizedList;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static reka.api.Path.dots;
import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.createEntry;
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
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.Path.Request;
import reka.api.Path.Response;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.run.EverythingSubscriber;
import reka.core.data.memory.MutableMemoryData;

@ChannelHandler.Sharable
public class HttpHostHandler extends SimpleChannelInboundHandler<MutableData> {
	
	private static final Path CLOSE_CHANNEL = dots("options.close");
	
	private static final Logger log = LoggerFactory.getLogger(HttpHostHandler.class);
	
	private final ConcurrentMap<String,List<Entry<Flow,Consumer<Flow>>>> paused = new ConcurrentHashMap<>();
	
	private final ConcurrentMap<String,Flow> flows = new ConcurrentHashMap<>();
	
	public boolean isEmpty() {
		return flows.isEmpty();
	}
	
	public HttpHostHandler add(String host, Flow flow) {
		flows.put(host, flow);
		List<Entry<Flow,Consumer<Flow>>> waiting = paused.remove(host);
		if (waiting != null) {
			synchronized (waiting) {
				log.info("resuming {} connections for host {} from newly deployed flow", waiting.size(), host);
				for (Entry<Flow, Consumer<Flow>> c : waiting) {
					c.getValue().accept(flow);
				}
			}
		}
		return this;
	}

	public boolean remove(String host) {
		if (flows.remove(host) != null) {
			log.info("removed host {}", host);
			paused.remove(host);
			return true;
		}
		return false;
	}

	public void pause(String host) {
		paused.putIfAbsent(host, synchronizedList(new ArrayList<>()));
	}


	public void resume(String host) {
		List<Entry<Flow,Consumer<Flow>>> waiting = paused.remove(host);
		if (waiting != null) {
			synchronized (waiting) {
				log.info("resuming {} connections for host {} from existing flow", waiting.size(), host);
				for (Entry<Flow, Consumer<Flow>> c : waiting) {
					c.getValue().accept(c.getKey());
				}
			}
		}
	}

	private Flow flowForHost(String host) {
		Flow flow = flows.get(host);
		return flow != null ? flow : flows.get("*");
	}
	
	private static class ChannelHandlerContextDataSubscriber implements EverythingSubscriber {

		private final long started = System.nanoTime();
		private final ChannelHandlerContext context;
		
		ChannelHandlerContextDataSubscriber(ChannelHandlerContext context) {
			this.context = context;
		}
		
		@Override
		public void ok(MutableData data) {
			ChannelFuture writeFuture = context.writeAndFlush(data);
			long took = (System.nanoTime() - started) / 1000;
			String statusStr = data.getString(Response.STATUS).orElse("");
			log.info("{} {} - {} {}µs", data.getString(Request.METHOD).orElse(""), data.getString(Request.PATH).orElse(""), statusStr, took);
			if (data.existsAt(CLOSE_CHANNEL)) {
				writeFuture.addListener(ChannelFutureListener.CLOSE);
			}
		}

		@Override
		public void halted() {

            MutableData data = MutableMemoryData.create()
				.put(Response.CONTENT, utf8("uh, oh it got halted :("))
				.put(Response.Headers.CONTENT_TYPE, utf8("text/plain"))
				.put(Response.STATUS, integer(500));
			
			context.writeAndFlush(data).addListener(ChannelFutureListener.CLOSE);
			
		}

		@Override
		public void error(Data data, Throwable error) {
			boolean acceptsHtml = data.getString(Request.Headers.ACCEPT).orElse("").contains("text/html");
			if (acceptsHtml) {
				context.writeAndFlush(htmlErrorMessage(data, error)).addListener(ChannelFutureListener.CLOSE);
			} else {
				context.writeAndFlush(jsonErrorMessage(data, error)).addListener(ChannelFutureListener.CLOSE);
			}
		}
		
		private static Data jsonErrorMessage(Data data, Throwable error) {
			Throwable t = unwrap(error);
			StringWriter stackTrace = new StringWriter();
			t.printStackTrace(new PrintWriter(stackTrace));
			return MutableMemoryData.create()
						.put(Response.CONTENT.add("data"), data)
						.putString(Response.CONTENT.add("message"), rootExceptionMessage(t))
						//.putString(Response.CONTENT.add("stacktrace"), stackTrace.toString())
					.putString(Response.Headers.CONTENT_TYPE, "application/json")
					.putInt(Response.STATUS, 500);

		}


		private static Data htmlErrorMessage(Data data, Throwable error) {
			Throwable t = unwrap(error);
			StringWriter stackTrace = new StringWriter();
			t.printStackTrace(new PrintWriter(stackTrace));
			return MutableMemoryData.create()
					.putString(Response.CONTENT, 
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
                                    escapeHtml4(data.toPrettyJson()) +
                                    "</pre>" +
                                    "</body></html>")
					.putString(Response.Headers.CONTENT_TYPE, "text/html")
					.putInt(Response.STATUS, 500);

		}
		
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext context, MutableData data) {

		String host = data.getString(Path.Request.HOST).orElse("localhost");

		Flow flow = flowForHost(host);
		
		if (flow == null) {
			noFlowFound(context, data, host);
			return;
		}
		
		if (paused.containsKey(host)) {
			List<Entry<Flow,Consumer<Flow>>> waiting = paused.get(host);
			synchronized (waiting) {
				// now we have the waiting list locked, check we are still frozen
				if (paused.containsKey(host)) {
					waiting.add(createEntry(flow, (newFlow) -> {
						executeFlow(context, newFlow, data, host);
					}));
					return;	
				}
			}
		}
		
		executeFlow(context, flow, data, host);
	}
	
	private void noFlowFound(ChannelHandlerContext context, Data data, String host) {
		log.debug("no flow registered for host {}", host);
		context.close();
	}
	
	private void executeFlow(ChannelHandlerContext context, Flow flow, MutableData data, String host) {
		//log.info("[{}] {} {}", flow.fullName(), data.getString(Request.METHOD).orElse(""), data.getString(Request.PATH).orElse(""));
		flow.run(listeningDecorator(context.executor()), data, new ChannelHandlerContextDataSubscriber(context));
	}

	private static String rootExceptionMessage(Throwable t) {
		return allExceptionMessages(t).iterator().next();
	}
	
	private static Collection<String> allExceptionMessages(Throwable tOriginal) {
		List<String> result = new ArrayList<>();
		
		Throwable t = tOriginal;
		
		while (t != null) {
			if (t.getMessage() != null) {
				result.add(t.getMessage());
			}
			t = t.getCause();
		}
		
		Collections.reverse(result);
		
		if (result.isEmpty()) {
			result.add("unknown error");
			log.error("error without any messages", tOriginal);
		}
		
		return result;
	}
	
}
