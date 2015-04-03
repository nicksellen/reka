package reka.net.http.server;

import static reka.util.Util.createEntry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reka.net.ChannelAttrs;
import reka.net.NetManager.HttpFlows;
import reka.net.NetSettings.Type;
import reka.util.Identity;

import com.google.common.base.Splitter;

@Sharable
public class HttpChannelSetup extends ChannelInboundHandlerAdapter implements ChannelSetup<HttpFlows> {
	
	private static final ChannelHandler DATASET_DECODER = new FullHttpToDataDecoder();
	private static final Splitter hostSplitter = Splitter.on(":").limit(2);
	
	private final ConcurrentMap<String,HttpFlows> flows = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,Identity> identities = new ConcurrentHashMap<>();
	private final ConcurrentMap<String,List<Entry<ChannelHandlerContext,FullHttpRequest>>> paused = new ConcurrentHashMap<>();

	private final ChannelGroup channels;
	private final int port;
	private final boolean ssl;
	
	public HttpChannelSetup(ChannelGroup channels, int port, boolean ssl) {
		this.channels = channels;
		this.port = port;
		this.ssl = ssl;
	}	

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof FullHttpRequest)) return;
		FullHttpRequest req = (FullHttpRequest) msg;
		
		String host = hostSplitter.split(HttpHeaders.getHost(req, "localhost")).iterator().next();
		
		if (!flows.containsKey(host)) {
			ctx.close();
			return;
		}
		
		if (paused.containsKey(host)) {
			paused.get(host).add(createEntry(ctx, req.retain()));
			return;
		}
		
		setup(ctx, host, req.retain());
	}
	
	private void setup(ChannelHandlerContext ctx, String host, FullHttpRequest req) {

		HttpFlows flow = flows.get(host);
		
		if (flow == null) {
			req.release();
			ctx.close();
			return;
		}
		
		Channel channel = ctx.channel();
		
		ctx.pipeline()
			.addLast("ds", DATASET_DECODER)
			.addLast("data", ssl ? DataToHttpEncoder.SSL : DataToHttpEncoder.NORMAL)
			.addLast("flow", new HttpFlowHandler(flow, ctx.channel()))
			.remove(this);
		
		channel.attr(ChannelAttrs.identity).set(identities.get(host));
		channel.attr(ChannelAttrs.host).set(host);
		channel.attr(ChannelAttrs.port).set(port);
		channel.attr(ChannelAttrs.type).set(Type.HTTP);
		
		channels.add(channel);
		
		ctx.fireChannelRead(req);
		
	}

	@Override
	public Runnable add(String host, Identity identity, HttpFlows flows) {
		this.flows.put(host, flows);
		this.identities.put(host, identity);
		return () -> {
			this.flows.remove(host);
			this.identities.remove(host);
		};
	}
	
	@Override
	public Runnable pause(String host) {
		paused.computeIfAbsent(host, unused -> new ArrayList<>());
		return () -> resume(host);
	}

	private void resume(String host) {
		List<Entry<ChannelHandlerContext, FullHttpRequest>> ctxs = paused.remove(host);
		if (ctxs == null) return;
		ctxs.forEach(e -> {
			ChannelHandlerContext ctx = e.getKey();
			FullHttpRequest req = e.getValue();
			setup(ctx, host, req);
		});
	}

	@Override
	public boolean isEmpty() {
		return flows.isEmpty();
	}

}
