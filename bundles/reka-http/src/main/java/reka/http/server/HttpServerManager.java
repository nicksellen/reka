package reka.http.server;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.http.server.HttpSettings.SslSettings;
import reka.http.websockets.WebsocketHandler;
import reka.http.websockets.WebsocketHandler.WebsocketHost;

import com.google.common.collect.ImmutableMap;

public class HttpServerManager {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final Logger logger = LoggerFactory.getLogger(HttpServerManager.class);

	private final EventLoopGroup nettyEventGroup;
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
		
	private final Object lock = new Object();
	private final Map<String,HttpSettings> deployed = new HashMap<>();
	
	private final boolean epoll;
	
	public HttpServerManager() {
		epoll = Epoll.isAvailable();
		nettyEventGroup = epoll ? new EpollEventLoopGroup() : new NioEventLoopGroup();
	}
	
	public EventLoopGroup nettyEventGroup() {
		return nettyEventGroup;
	}
	
	private class PortHandler {
		
		private final int port;
		private final SslSettings sslSettings;
		private final HttpHostHandler httpHandler = new HttpHostHandler();;
		private final WebsocketHandler websocketHandler = new WebsocketHandler();
		private final HttpOrWebsocket httpOrWebsocketHandler;
		private final ChannelInitializer<SocketChannel> initializer;
		
		private volatile Channel channel;
		
		PortHandler(int port, SslSettings sslSettings) {
			this.port = port;
			this.sslSettings = sslSettings;
			httpOrWebsocketHandler = new HttpOrWebsocket(httpHandler, websocketHandler, sslSettings != null);

			if (sslSettings != null) {
				initializer = new HttpsInitializer(httpOrWebsocketHandler, sslSettings.certChainFile(), sslSettings.keyFile());
			} else {
				initializer = new HttpInitializer(httpOrWebsocketHandler);
			}
		}
		
		public PortHandler httpAdd(String host, Flow flow) {
			httpHandler.add(host, flow);
			if (channel == null) start();
			return this;
		}

		public void httpPause(String host) {
			httpHandler.pause(host);
		}

		public void httpResume(String host) {
			httpHandler.resume(host);
		}

		public PortHandler websocketAdd(String host, WebsocketTriggers d) {
			websocketHandler.add(host, d);
			if (channel == null) {
				start();
			}
			return this;
		}
		
		public PortHandler websocketRemove(String host) {
			if (websocketHandler.remove(host)) {
				if (isEmpty()) stop();
			}
			return this;
		}
		
		public boolean isSsl() {
			return sslSettings != null;
		}
		
		public SslSettings sslSettings() {
			return sslSettings;
		}
		
		public PortHandler httpRemove(String host) {
			if (httpHandler.remove(host)) {
				if (isEmpty()) stop();
			}
			return this;
		}
		
		private boolean isEmpty() {
			return httpHandler.isEmpty() && websocketHandler.isEmpty();
		}
		
		private void stop() {
			if (channel != null) {
				try {
					channel.close().sync();
					logger.info("closed port {}", port);
				} catch (InterruptedException e) {
					throw unchecked(e);
				}
				channel = null;
			}
		}
		
		private void start() {
			
			ServerBootstrap bootstrap = new ServerBootstrap()
				.localAddress(port)
				.group(nettyEventGroup)
				.option(ChannelOption.SO_BACKLOG, 1024)
		    	.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
				.childHandler(initializer);
			
			if (epoll) {
				bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
				bootstrap.channel(EpollServerSocketChannel.class);
			} else {
				bootstrap.channel(NioServerSocketChannel.class);
			}
			
			try {
				
				channel = bootstrap.bind().sync().channel();
				
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
	
	public static class WebsocketTriggers {
		
		private final List<IdentityKey<Object>> topicKeys = new ArrayList<>();
		
		private final List<Flow> onConnect = new ArrayList<>();
		private final List<Flow> onDisconnect = new ArrayList<>();
		private final List<Flow> onMessage = new ArrayList<>();
		
		public WebsocketTriggers topic(IdentityKey<Object> topicKey) {
			topicKeys.add(topicKey);
			return this;
		}
		
		public WebsocketTriggers connect(Flow flow) {
			onConnect.add(flow);
			return this;
		}
		
		public WebsocketTriggers disconnect(Flow flow) {
			onDisconnect.add(flow);
			return this;
		}
		
		public WebsocketTriggers message(Flow flow) {
			onMessage.add(flow);
			return this;
		}
		
		public List<IdentityKey<Object>> topicKeys() {
			return topicKeys;
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
	
	public void websocket(HttpSettings settings, Consumer<WebsocketHost> c) {
		PortHandler handler = handlers.get(settings.port());
		if (handler == null) return;
		handler.websocketHandler.forHost(settings.host(), c);
	}
	
	public void deployWebsocket(String identity, HttpSettings settings, Consumer<WebsocketTriggers> deploy) {
		
		synchronized (lock) {
			WebsocketTriggers websocketTriggers = new WebsocketTriggers();
			deploy.accept(websocketTriggers);
			deploy(identity, settings, handler -> {
				handler.websocketAdd(settings.host(), websocketTriggers);
			});
		}
		
	}
	
	public void deployHttp(String identity, Flow flow, HttpSettings settings) {
		log.debug("deploying [{}] with {} {}:{}", identity, flow.fullName(), settings.host(), settings.port());
		deploy(identity, settings, handler -> {
			handler.httpAdd(settings.host(), flow);
		});
	}

	private void deploy(String identity, HttpSettings settings, Consumer<PortHandler> consumer) {
		
		synchronized (lock) {
			
			HttpSettings previous = deployed.put(identity, settings);
			
			boolean removePrevious = false;

			if (previous != null) {
				boolean hostChanged = !previous.host().equals(settings.host());
				boolean portChanged = previous.port() != settings.port();
				removePrevious = hostChanged || portChanged;
			}
			
			PortHandler portHandler = handlers.get(settings.port());
			
			if (portHandler != null) {
				if ((portHandler.isSsl() != settings.isSsl()) || 
				    (portHandler.isSsl() && settings.isSsl() && 
				     !portHandler.sslSettings().equals(settings.sslSettings()))) {
					throw runtime("must have same ssl settings (2)");
				}
			} else {
				portHandler = new PortHandler(settings.port(), settings.sslSettings());
				handlers.put(settings.port(), portHandler);
			}
			
			consumer.accept(portHandler);
			
			if (removePrevious) {
				PortHandler h = handlers.get(previous.port());
				if (h != null) {
					switch (settings.type()) {
					case HTTP:
						h.httpRemove(previous.host());	
						break;
					case WEBSOCKET:
						h.websocketRemove(previous.host());
						break;
					}
					if (h.isEmpty()) handlers.remove(previous.port());
				}
			}
			
		}
	}
	
	public void undeploy(String identity, int undeployVersion) {
		
		synchronized (lock) {
			
			HttpSettings settings = deployed.get(identity);
			
			if (settings == null) {
				log.debug("   it didn't seem to actually be deployed ({} were though)", deployed.keySet());
				return;
			}
			
			if (settings.applicationVersion() > undeployVersion) {
				log.info("ignoring request to undeploy version {} as we're on version {}", undeployVersion, settings.applicationVersion());
				return;
			}
			
			deployed.remove(identity);
			
			PortHandler handler = handlers.get(settings.port());
			if (handler != null) {
				switch (settings.type()) {
				case HTTP:
					handler.httpRemove(settings.host());	
					break;
				case WEBSOCKET:
					handler.websocketRemove(settings.host());
					break;
				}
				if (handler.isEmpty()) handlers.remove(settings.port());
			}
			
		}
	}
	
	public void pause(String identity, int version) {
		
		synchronized (lock) {
			
			HttpSettings settings = deployed.get(identity);
		
			if (settings == null) {
				return;
			}
			
			if (settings.applicationVersion() > version) {
				log.info("tried to pause version {} but we're running a new version {}", version, settings.applicationVersion());
				return;
			}
			
			PortHandler handler = handlers.get(settings.port());
			if (handler != null) {
				log.debug("pausing [{}]", identity);
				handler.httpPause(settings.host());
			}
			
		}
	}

	public void resume(String identity, int version) {
		
		synchronized (lock) {
			
			HttpSettings settings = deployed.get(identity);
		
			if (settings == null) {
				return;
			}
			
			if (settings.applicationVersion() > version) {
				log.info("tried to resume version {} but we're running a new version {}", version, settings.applicationVersion());
				return;
			}
			
			PortHandler handler = handlers.get(settings.port());
			if (handler != null) {
				log.debug("resuming [{}]", identity);
				handler.httpResume(settings.host());
			}
			
		}
	}

	public void shutdown() {
		synchronized(lock) {
			handlers.values().forEach(port -> port.stop());
		}
	}

}
