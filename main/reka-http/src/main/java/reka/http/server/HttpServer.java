package reka.http.server;

import static reka.util.Util.unchecked;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.flow.Flow;
import reka.http.websockets.WebsocketHandler;
import reka.util.Util;

import com.google.common.collect.ImmutableMap;

public class HttpServer {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class.getSimpleName());

	private final NioEventLoopGroup group = new NioEventLoopGroup();
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
		
	private final Object lock = new Object();
	private final Map<String,HttpSettings> deployed = new HashMap<>();
	
	public NioEventLoopGroup group() {
		return group;
	}
	
	private class PortHandler {
		
		private final int port;
		private final HttpVhostHandler httpHandler = new HttpVhostHandler();;
		private final WebsocketHandler websocketHandler = new WebsocketHandler();
		private final HttpOrWebsocket handler;
		
		private volatile Channel channel;
		
		PortHandler(int port) {
			this.port = port;
			handler = new HttpOrWebsocket(httpHandler, websocketHandler);
		}
		
		public boolean isEmpty() {
			return httpHandler.isEmpty();
		}
		
		public PortHandler addHttp(String host, Flow flow) {
			httpHandler.add(host, flow);
			if (channel == null) start();
			return this;
		}

		public void broadcastWebsocket(String host, String msg) {
			websocketHandler.broadcast(host, msg);
		}
		
		public void sendWebsocket(String host, String to, String msg) {
			websocketHandler.send(host, to, msg);
		}

		public PortHandler addWebsocket(String host, WebsocketHandlers d) {
			websocketHandler.add(host, d);
			if (channel == null) {
				start();
			}
			return this;
		}
		
		public PortHandler remove(String host) {
			if (httpHandler.remove(host)) {
				if (httpHandler.isEmpty()) {
					stop();
				}
			}
			return this;
		}
		
		private void stop() {
			if (channel != null) {
				try {
					channel.close().sync();
					logger.info("closed port {}", port);
				} catch (InterruptedException e) {
					throw Util.unchecked(e);
				}
				channel = null;
			}
		}
		
		private void start() {
			
			ServerBootstrap bootstrap = new ServerBootstrap()
				.localAddress(port)
				.group(group)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.SO_REUSEADDR, true)
				//.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				//.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(
								new HttpRequestDecoder(),
								//new HttpObjectAggregator(1024 * 1024 * 10), // 10mb
								new HttpObjectAggregator(1024 * 1024 * 500), // 500mb
								//new HttpContentCompressor(),
								new HttpResponseEncoder(),
								new ChunkedWriteHandler(), 
								handler);
					}
				});

			try {
				channel = bootstrap.bind().sync().channel();
				//channel.closeFuture();
				logger.info("opened port {}", port);
				
			} catch (InterruptedException e) {
				throw unchecked(e, "could not bind port %d", port);
			}
		}
	}
	
	public Map<String,HttpSettings> deployed() {
		synchronized (lock) {
			return ImmutableMap.copyOf(deployed);	
		}
	}
	
	public static class WebsocketHandlers {
		
		private final List<Flow> onConnect = new ArrayList<>();
		private final List<Flow> onDisconnect = new ArrayList<>();
		private final List<Flow> onMessage = new ArrayList<>();
		
		public WebsocketHandlers connect(Flow flow) {
			onConnect.add(flow);
			return this;
		}
		
		public WebsocketHandlers disconnect(Flow flow) {
			onDisconnect.add(flow);
			return this;
		}
		
		public WebsocketHandlers message(Flow flow) {
			onMessage.add(flow);
			return this;
		}
		
		public List<Flow> onConnect() {
			return onConnect;
		}
		
		public List<Flow> onDisconnect() {
			return onDisconnect;
		}
		
		public List<Flow> onMessage() {
			return onMessage;
		}
		
	}
	
	// TODO: I don't like these here....!
	
	public void broadcastWebsocket(HttpSettings settings, String msg) {
		PortHandler handler = handlers.get(settings.port());
		if (handler == null) return;
		handler.broadcastWebsocket(settings.host(), msg);
	}
	
	public void sendWebsocket(HttpSettings settings, String to, String msg) {
		PortHandler handler = handlers.get(settings.port());
		if (handler == null) return;
		handler.sendWebsocket(settings.host(), to, msg);
	}
	
	public void deployWebsocket(String identity, HttpSettings settings, Consumer<WebsocketHandlers> deploy) {
		
		synchronized (lock) {
			WebsocketHandlers websocketHandlers = new WebsocketHandlers();
			deploy.accept(websocketHandlers);
			
			@SuppressWarnings("unused")
			HttpSettings previous = deployed.put(identity, settings);
			
			deploy(identity, settings, handler -> {
				handler.addWebsocket(settings.host(), websocketHandlers);
			});
			
			// TODO: add websocket removals...
			
		}
		
	}
	
	public void deployHttp(String identity, Flow flow, HttpSettings settings) {
		log.debug("deploying [{}] with {} {}:{}", identity, flow.fullName(), settings.host(), settings.port());
		deploy(identity, settings, handler -> {
			handler.addHttp(settings.host(), flow);
		});
	}

	private void deploy(String identity, HttpSettings settings, Consumer<PortHandler> handler) {
		
		synchronized (lock) {
			
			HttpSettings previous = deployed.put(identity, settings);
			
			boolean removePrevious = false;
			
			if (previous != null) {
				boolean hostChanged = !previous.host().equals(settings.host());
				boolean portChanged = previous.port() != settings.port();
				removePrevious = hostChanged || portChanged;
			}
			
			handler.accept(handlers.computeIfAbsent(settings.port(), (port) -> new PortHandler(port)));
			
			if (removePrevious) {
				PortHandler h = handlers.get(previous.port());
				if (h != null) {
					h.remove(settings.host());
					if (h.isEmpty()) {
						handlers.remove(settings.port());
					}
				}
			}
			
		}
	}
	
	public void undeploy(String identity) {
		log.debug("undeploying [{}]", identity);
		
		synchronized (lock) {
			
			HttpSettings settings = deployed.remove(identity);
		
			if (settings == null) {
				log.debug("   it didn't seem to actually be deployed ({} were though)", deployed.keySet());
				return;
			}
			
			PortHandler handler = handlers.get(settings.port());
			if (handler != null) {
				handler.remove(settings.host());
				if (handler.isEmpty()) {
					handlers.remove(settings.port());
				}
			}
			
		}
	}

}
