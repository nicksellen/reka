package reka.net;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import static reka.util.Util.unsupported;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.PortChecker;
import reka.api.IdentityKey;
import reka.api.flow.Flow;
import reka.net.NetSettings.SslSettings;
import reka.net.NetSettings.Type;
import reka.net.http.HostAndPort;
import reka.net.http.server.HttpHostHandler;
import reka.net.http.server.HttpInitializer;
import reka.net.http.server.HttpOrWebsocket;
import reka.net.http.server.HttpsInitializer;
import reka.net.socket.SocketHandler;
import reka.net.websockets.WebsocketHandler;

import com.google.common.collect.ImmutableMap;

public class NetServerManager {
	
	public static void main(String[] args) {
		
		Optional<String> a = Optional.of("name");
		Optional<String> b = Optional.of("name");
		
		System.out.printf("a==b? : %s\n", a.equals(b));
		
	}
	
	private static final Logger log = LoggerFactory.getLogger(NetServerManager.class);

	private final EventLoopGroup nettyEventGroup;
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
		
	private final Map<String,NetSettings> deployed = new HashMap<>();
	
	private final boolean epoll;
	
	private final Class<? extends ServerChannel> nettyServerChannelType;
	private final Class<? extends Channel> nettyClientChannelType;
	
	public NetServerManager() {
		epoll = Epoll.isAvailable();
		if (epoll) {
			nettyServerChannelType = EpollServerSocketChannel.class;
			nettyClientChannelType = EpollSocketChannel.class;
			nettyEventGroup = new EpollEventLoopGroup();
		} else {
			nettyServerChannelType = NioServerSocketChannel.class;
			nettyClientChannelType = NioSocketChannel.class;
			nettyEventGroup = new NioEventLoopGroup();
		}
	}
	
	public EventLoopGroup nettyEventGroup() {
		return nettyEventGroup;
	}
	
	public Class<? extends ServerChannel> nettyServerChannelType() {
		return nettyServerChannelType;
	}
	
	public Class<? extends Channel> nettyChannelType() {
		return nettyClientChannelType;
	}
	
	private final class HttpPortHandler extends PortHandler {

		private final ChannelInitializer<SocketChannel> initializer;
		
		private final HttpHostHandler httpHandler;
		private final WebsocketHandler websocketHandler;
		private final HttpOrWebsocket httpOrWebsocketHandler;
		
		HttpPortHandler(int port, SslSettings sslSettings) {
			super(port, sslSettings);
			
			httpHandler = new HttpHostHandler();
			websocketHandler =  new WebsocketHandler();
			httpOrWebsocketHandler = new HttpOrWebsocket(httpHandler, websocketHandler, sslSettings != null);
			if (sslSettings != null) {
				initializer = new HttpsInitializer(httpOrWebsocketHandler, sslSettings.certChainFile(), sslSettings.keyFile());
			} else {
				initializer = new HttpInitializer(httpOrWebsocketHandler);
			}
		}

		@Override
		protected ChannelInitializer<SocketChannel> initializer() {
			return initializer;
		}

		@Override
		public boolean isHttp() {
			return true;
		}

		@Override
		public PortHandler httpAdd(String host, Flow flow) {
			httpHandler.add(host, flow);
			start();
			return this;
		}

		@Override
		public PortHandler websocketAdd(String host, SocketTriggers triggers) {
			websocketHandler.add(host, triggers);
			start();
			return this;
		}

		@Override
		public void pause(NetSettings settings) {
			if (settings.type() != Type.HTTP) return;
			httpHandler.pause(settings.host().get());
		}

		@Override
		public void resume(NetSettings settings) {
			if (settings.type() != Type.HTTP) return;
			httpHandler.resume(settings.host().get());
		}

		@Override
		public PortHandler remove(NetSettings settings) {
			switch (settings.type()) {
			case HTTP:
				httpRemove(settings.host().get());
				break;
			case WEBSOCKET:
				websocketRemove(settings.host().get());
				break;
			default:
				throw runtime("cannot remove %s from %s", settings.type(), getClass().getSimpleName());
			}
			return null;
		}

		private PortHandler httpRemove(String host) {
			if (httpHandler.remove(host)) {
				if (isEmpty()) stop();
			}
			return this;
		}

		private PortHandler websocketRemove(String host) {
			if (websocketHandler.remove(host)) {
				if (isEmpty()) stop();
			}
			return this;
		}
		
		@Override
		public boolean isEmpty() {
			return httpHandler.isEmpty() && websocketHandler.isEmpty();
		}

		@Override
		public PortHandler socketSet(SocketTriggers triggers) {
			throw unsupported();
		}

		@Override
		public void channel(NetSettings settings, String id, Consumer<Channel> c) {
			checkArgument(settings.type() == Type.WEBSOCKET, "cannot get channels for %s", settings.type());
			settings.host().ifPresent(host -> {
				websocketHandler.forHost(host, h -> {
					h.channel(id).ifPresent(c);
				});	
			});
		}

		@Override
		public void channels(NetSettings settings, Consumer<ChannelGroup> c) {
			checkArgument(settings.type() == Type.WEBSOCKET, "cannot get channels for %s", settings.type());
			settings.host().ifPresent(host -> {
				websocketHandler.forHost(host, h -> {
					c.accept(h.channels);
				});	
			});
		}
		
	}
	
	private final class SocketPortHandler extends PortHandler {

		private final SocketHandler socketHandler;
		private final ChannelInitializer<SocketChannel> initializer;
		
		private volatile boolean isSet = false;
		
		SocketPortHandler(int port, SslSettings sslSettings) {
			super(port, sslSettings);
			socketHandler = new SocketHandler();
			if (sslSettings != null) {
				initializer = new SslSocketInitializer(socketHandler, sslSettings.certChainFile(), sslSettings.keyFile());
			} else {
				initializer = new SocketInitializer(socketHandler);
			}
		}

		@Override
		protected ChannelInitializer<SocketChannel> initializer() {
			return initializer;
		}

		@Override
		public PortHandler socketSet(SocketTriggers triggers) {
			isSet = true;
			socketHandler.setTriggers(triggers);
			start();
			return this;
		}

		@Override
		public boolean isHttp() {
			return false;
		}

		@Override
		public PortHandler httpAdd(String host, Flow flow) {
			throw unsupported();
		}

		@Override
		public PortHandler websocketAdd(String host, SocketTriggers triggers) {
			throw unsupported();
		}

		@Override
		public void pause(NetSettings settings) {
			throw unsupported();
		}

		@Override
		public void resume(NetSettings settings) {
			throw unsupported();
		}

		@Override
		public boolean isEmpty() {
			return !isSet;
		}

		@Override
		public PortHandler remove(NetSettings settings) {
			checkArgument(settings.type() == Type.SOCKET);
			isSet = false;
			channels(settings, channels -> {
				channels.disconnect();
			});
			stop();
			return this;
		}

		@Override
		public void channel(NetSettings settings, String id, Consumer<Channel> c) {
			checkArgument(settings.type() == Type.SOCKET);
			socketHandler.channel(id).ifPresent(c);
		}

		@Override
		public void channels(NetSettings settings, Consumer<ChannelGroup> c) {
			checkArgument(settings.type() == Type.SOCKET);
			c.accept(socketHandler.channels());
		}
		
	}
	
	private abstract class PortHandler {
		
		private final int port;
		private final SslSettings sslSettings;
				
		protected volatile Channel channel;
		
		protected PortHandler(int port, SslSettings sslSettings) {
			this.port = port;
			this.sslSettings = sslSettings;
		}
		
		protected abstract ChannelInitializer<SocketChannel> initializer();
		
		public abstract boolean isHttp();
		public abstract boolean isEmpty();
		
		public abstract PortHandler httpAdd(String host, Flow flow);
		public abstract PortHandler websocketAdd(String host, SocketTriggers triggers);
		public abstract PortHandler socketSet(SocketTriggers triggers);
		
		public abstract void pause(NetSettings settings);
		public abstract void resume(NetSettings settings);
		
		public abstract PortHandler remove(NetSettings settings);
		
		public SslSettings sslSettings() {
			return sslSettings;
		}
		
		protected void stop() {
			if (channel != null) {
				try {
					channel.close().sync();
					log.info("closed port {}", port);
				} catch (InterruptedException e) {
					throw unchecked(e);
				}
				channel = null;
			}
		}
		
		protected void start() {
			
			if (channel != null) return;
			
			ServerBootstrap bootstrap = new ServerBootstrap()
				.localAddress(port)
				.group(nettyEventGroup)
				.channel(nettyServerChannelType)
				.option(ChannelOption.SO_BACKLOG, 1024)
		    	.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.TCP_NODELAY, true)
				.option(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
				.childHandler(initializer());
			
			if (epoll) {
				// TODO: this needs more thought, it causes me to keep running multiple servers at the mo!
				//bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
			}
			
			try {
				
				channel = bootstrap.bind().sync().channel();
				
				log.info("opened port {}", port);
				
			} catch (InterruptedException e) {
				throw unchecked(e, "could not bind port %d", port);
			}
		}

		public abstract void channel(NetSettings settings, String id, Consumer<Channel> c);
		public abstract void channels(NetSettings settings, Consumer<ChannelGroup> c);
		
	}
	
	public Map<String,NetSettings> deployed() {
		return ImmutableMap.copyOf(deployed);
	}
	
	public static class SocketTriggers {
		
		private final List<IdentityKey<Object>> topicKeys = new ArrayList<>();
		
		private final List<Flow> onConnect = new ArrayList<>();
		private final List<Flow> onDisconnect = new ArrayList<>();
		private final List<Flow> onMessage = new ArrayList<>();
		
		public SocketTriggers topic(IdentityKey<Object> topicKey) {
			topicKeys.add(topicKey);
			return this;
		}
		
		public SocketTriggers onConnect(Flow flow) {
			onConnect.add(flow);
			return this;
		}
		
		public SocketTriggers onDisconnect(Flow flow) {
			onDisconnect.add(flow);
			return this;
		}
		
		public SocketTriggers onMessage(Flow flow) {
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
	
	public void channel(NetSettings settings, String channelId, Consumer<Channel> c) {
		PortHandler handler = handlers.get(settings.port());
		if (handler == null) return;
		handler.channel(settings, channelId, c);
	}
	
	public void channels(NetSettings settings, Consumer<ChannelGroup> c) {
		PortHandler handler = handlers.get(settings.port());
		if (handler == null) return;
		handler.channels(settings, c);
	}
	
	public boolean isAvailable(String applicationIdentity, HostAndPort listen) {
		return deployed.values().stream().allMatch(settings -> {
			
			if (settings.applicationIdentity().equals(applicationIdentity)) {
				// the same application, we can reuse whatever
				return true;
			}
			
			if (listen.port() != settings.port()) {
				// different port, doesn't matter
				return true;
			}
			
			if (settings.host().isPresent()) {
				// so long as they're different hosts it's ok
				return !listen.host().equals(settings.host().get());
			} else {
				// we don't have a host set, i.e. it doesn't support multiple hosts
				return false;
			}
			
		});
	}
	
	public boolean isAvailable(String applicationIdentity, int port) {
		return deployed.values().stream().allMatch(settings -> {
			return settings.applicationIdentity().equals(applicationIdentity) || port != settings.port();
		});
	}
	
	public void deployHttp(String id, Flow flow, NetSettings settings) {
		checkArgument(settings.type() == Type.HTTP, "settings type must be %s", Type.HTTP.toString());
		checkArgument(settings.host().isPresent(), "must include host");
		log.debug("deploying [{}] with {} {}:{}", id, flow.fullName(), settings.host(), settings.port());
		deploy(id, settings, handler -> {
			handler.httpAdd(settings.host().get(), flow);
		});
	}

	public void deployWebsocket(String id, NetSettings settings, Consumer<SocketTriggers> deploy) {
		checkArgument(settings.type() == Type.WEBSOCKET, "settings type must be %s", Type.WEBSOCKET.toString());
		checkArgument(settings.host().isPresent(), "must include host");
		SocketTriggers socketTriggers = new SocketTriggers();
		deploy.accept(socketTriggers);
		deploy(id, settings, handler -> {
			handler.websocketAdd(settings.host().get(), socketTriggers);
		});
	}
	
	public void deploySocket(String id, NetSettings settings, Consumer<SocketTriggers> deploy) {
		checkArgument(settings.type() == Type.SOCKET, "settings type must be %s", Type.SOCKET.toString());
		SocketTriggers socketTriggers = new SocketTriggers();
		deploy.accept(socketTriggers);
		checkArgument(isAvailable(settings.applicationIdentity(), settings.port()), "port unavailable");
		deploy(id, settings, handler -> {
			handler.socketSet(socketTriggers);
		});
	}

	private void deploy(String id, NetSettings settings, Consumer<PortHandler> consumer) {
		
		PortHandler portHandler = handlers.get(settings.port());

		if (portHandler != null) {
			if (!portHandler.isHttp() && settings.type() != Type.SOCKET) {
				// non multihost, but we need it to be
				throw runtime("cannot deploy %s to a non-multihost handler", settings.type());
			}
		}
		
		NetSettings previous = deployed.put(id, settings);
		
		boolean removePrevious = false;
		
		if (previous != null) {
			boolean hostChanged = !previous.host().equals(settings.host());
			boolean portChanged = previous.port() != settings.port();
			removePrevious = hostChanged || portChanged;
		}
		
		if (portHandler != null) {
			if (!Objects.equals(portHandler.sslSettings(), settings.sslSettings())) {
				throw runtime("must have same ssl settings");
			}
		} else {
			if (settings.type() == Type.SOCKET) {
				portHandler = new SocketPortHandler(settings.port(), settings.sslSettings());
			} else {
				portHandler = new HttpPortHandler(settings.port(), settings.sslSettings());
			}
			handlers.put(settings.port(), portHandler);
		}
		
		consumer.accept(portHandler);
		
		if (removePrevious) {
			PortHandler h = handlers.get(previous.port());
			if (h != null) {
				h.remove(settings);
				if (h.isEmpty()) handlers.remove(previous.port());
			}
		}
	}
	
	public void undeploy(String id, int undeployVersion) {
		
		NetSettings settings = deployed.get(id);
		
		if (settings == null) {
			log.debug("   it didn't seem to actually be deployed ({} were though)", deployed.keySet());
			return;
		}
		
		if (settings.applicationVersion() > undeployVersion) {
			log.info("ignoring request to undeploy version {} as we're on version {}", undeployVersion, settings.applicationVersion());
			return;
		}
		
		deployed.remove(id);
		
		PortHandler handler = handlers.get(settings.port());
		if (handler != null) {
			handler.remove(settings);
			if (handler.isEmpty()) handlers.remove(settings.port());
		}
			
	}
	
	public void pause(String id, int version) {
		NetSettings settings = deployed.get(id);
	
		if (settings == null) {
			return;
		}
		
		if (settings.applicationVersion() > version) {
			log.info("tried to pause version {} but we're running a new version {}", version, settings.applicationVersion());
			return;
		}
		
		PortHandler handler = handlers.get(settings.port());
		if (handler != null) {
			log.debug("pausing [{}]", id);
			handler.pause(settings);
		}
	}

	public void resume(String id, int version) {
			
		NetSettings settings = deployed.get(id);
	
		if (settings == null) {
			return;
		}
		
		if (settings.applicationVersion() > version) {
			log.info("tried to resume version {} but we're running a new version {}", version, settings.applicationVersion());
			return;
		}
		
		PortHandler handler = handlers.get(settings.port());
		if (handler != null) {
			log.debug("resuming [{}]", id);
			handler.resume(settings);
		}
		
	}

	public void shutdown() {
		handlers.values().forEach(port -> port.stop());
		try {
			nettyEventGroup.shutdownGracefully().await();
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
	}

	public final PortChecker portChecker = new PortChecker() {
		
		@Override
		public boolean check(String applicationIdentity, int port, Optional<String> host) {
			if (host.isPresent()) {
				return isAvailable(applicationIdentity, new HostAndPort(host.get(), port));
			} else {
				return isAvailable(applicationIdentity, port);
			}
		}
	};

}
