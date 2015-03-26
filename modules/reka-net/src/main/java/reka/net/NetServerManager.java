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
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Identity;
import reka.PortChecker;
import reka.api.flow.Flow;
import reka.core.runtime.NoFlow;
import reka.net.NetSettings.SslSettings;
import reka.net.NetSettings.Type;
import reka.net.http.HostAndPort;
import reka.net.http.server.HttpInitializer;
import reka.net.http.server.HttpOrWebsocket;
import reka.net.socket.SocketFlowHandler;
import reka.util.AsyncShutdown;

import com.google.common.collect.ImmutableMap;

public class NetServerManager {
	
	private static final Logger log = LoggerFactory.getLogger(NetServerManager.class);

	private final EventLoopGroup nettyEventGroup;
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
		
	private final Map<Identity,NetSettings> deployed = new HashMap<>();
	
	private final boolean epoll;
	
	private final Class<? extends ServerChannel> nettyServerChannelType;
	private final Class<? extends Channel> nettyClientChannelType;

	private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private final SafeChannelGroup safeChannels = new SafeChannelGroup(channels);
	
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
		
		private final HttpOrWebsocket handler;
		
		HttpPortHandler(int port, SslSettings sslSettings) {
			super(port, sslSettings);
			handler = new HttpOrWebsocket(channels, port, sslSettings != null);
			initializer = new HttpInitializer(handler, sslSettings);
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
		public PortHandler httpAdd(Identity identity, String host, HttpFlows flows) {
			handler.addHttp(identity, host, flows);
			start();
			return this;
		}

		@Override
		public PortHandler websocketAdd(Identity identity, String host, SocketFlows flows) {
			handler.addWebsocket(identity, host, flows);
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
			if (handler.removeHttp(host)) {
				if (isEmpty()) shutdownAndWait();
			}
			return this;
		}

		private PortHandler websocketRemove(String host) {
			if (handler.removeWebsocket(host)) {
				if (isEmpty()) shutdownAndWait();
			}
			return this;
		}
		
		@Override
		public boolean isEmpty() {
			return handler.isEmpty();
		}

		@Override
		public PortHandler socketSet(Identity identity, SocketFlows flows) {
			throw runtime("we are doing http on this port, undeploy this first before using it for sockets. sockets don't have a host so they can't co-exist with http.");
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

		private final SocketFlowHandler socketHandler;
		private final ChannelInitializer<SocketChannel> initializer;
		
		private volatile boolean isSet = false;
		
		SocketPortHandler(int port, SslSettings sslSettings) {
			super(port, sslSettings);
			socketHandler = new SocketFlowHandler();
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
		public PortHandler socketSet(Identity identity, SocketFlows flows) {
			isSet = true;
			socketHandler.setFlows(flows);
			start();
			return this;
		}

		@Override
		public boolean isHttp() {
			return false;
		}

		@Override
		public PortHandler httpAdd(Identity identity, String host, HttpFlows flows) {
			throw unsupported();
		}

		@Override
		public PortHandler websocketAdd(Identity identity, String host, SocketFlows flows) {
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
			shutdownAndWait();
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
	
	private abstract class PortHandler implements AsyncShutdown {
		
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
		
		public abstract PortHandler httpAdd(Identity identity, String host, HttpFlows flows);
		public abstract PortHandler websocketAdd(Identity identity, String host, SocketFlows flows);
		public abstract PortHandler socketSet(Identity identity, SocketFlows flows);
		
		public abstract void pause(NetSettings settings);
		public abstract void resume(NetSettings settings);
		
		public abstract PortHandler remove(NetSettings settings);
		
		public SslSettings sslSettings() {
			return sslSettings;
		}
		
		public void shutdown(Result res) {
			if (channel != null) {
				channel.close().addListener(future -> {
					log.info("closed port {}", port);
					if (future.isSuccess()) {
						res.complete();
					} else {
						res.completeExceptionally(future.cause());
					}
				});
				channel = null;
			} else {
				res.complete();
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
				.childOption(ChannelOption.AUTO_READ, false)
				.childHandler(initializer());
			
			if (epoll) {
				// TODO: this needs more thought, it causes me to keep running multiple servers at the mo!
				//bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
			}
			
			try {
				log.info("binding to port {}", port);
				channel = bootstrap.bind().sync().channel();
				channels.add(channel);
				log.info("opened port {}", port);
				
			} catch (Throwable t) {
				throw unchecked(t, "could not bind port %d", port);
			}
		}

		public abstract void channel(NetSettings settings, String id, Consumer<Channel> c);
		public abstract void channels(NetSettings settings, Consumer<ChannelGroup> c);
		
	}
	
	public Map<Identity,NetSettings> deployed() {
		return ImmutableMap.copyOf(deployed);
	}
	
	public SafeChannelGroup channels() {
		return safeChannels;
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
	
	public void deployHttp(Identity id, NetSettings settings, HttpFlows flows) {
		checkArgument(settings.type() == Type.HTTP, "settings type must be %s", Type.HTTP.toString());
		checkArgument(settings.host().isPresent(), "must include host");
		log.debug("deploying [{}] to {}:{}", id, settings.host(), settings.port());
		deploy(id, settings, handler -> {
			handler.httpAdd(settings.host().get(), flows);
		});
	}

	public void deployWebsocket(Identity id, NetSettings settings, SocketFlows flows) {
		checkArgument(settings.type() == Type.WEBSOCKET, "settings type must be %s", Type.WEBSOCKET.toString());
		checkArgument(settings.host().isPresent(), "must include host");
		deploy(id, settings, handler -> {
			handler.websocketAdd(settings.host().get(), flows);
		});
	}
	
	public void deploySocket(Identity id, NetSettings settings, SocketFlows flows) {
		checkArgument(settings.type() == Type.SOCKET, "settings type must be %s", Type.SOCKET.toString());
		deploy(id, settings, handler -> {
			handler.socketSet(flows);
		});
	}

	private void deploy(Identity identity, NetSettings settings, Consumer<PortHandler> consumer) {
		
		PortHandler portHandler = handlers.get(settings.port());

		if (portHandler != null) {
			if (!portHandler.isHttp() && settings.type() != Type.SOCKET) {
				// non multihost, but we need it to be
				throw runtime("cannot deploy %s to a non-multihost handler", settings.type());
			}
		}
		
		NetSettings previous = deployed.put(identity, settings);
		
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
	
	public void undeploy(Identity identity, int undeployVersion) {
		
		NetSettings settings = deployed.get(identity);
		
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
			handler.remove(settings);
			if (handler.isEmpty()) handlers.remove(settings.port());
		}
			
	}
	
	public void pause(Identity identity, int version) {
		NetSettings settings = deployed.get(identity);
	
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
			handler.pause(settings);
		}
	}

	public void resume(Identity identity, int version) {
			
		NetSettings settings = deployed.get(identity);
	
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
			handler.resume(settings);
		}
		
	}

	public void shutdown(AsyncShutdown.Result res) {
		channels.close().addListener(future1 -> {
			
			// just ignore the errors from the channels
			
			nettyEventGroup.shutdownGracefully().addListener(future2 -> {
				if (future2.isSuccess()) {
					res.complete();
				} else {
					res.completeExceptionally(future2.cause());
				}
			});
			
		});
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
	



	public static class HttpFlows {
		
		private final Flow onMessage;
		
		public HttpFlows(Flow onMessage) {
			this.onMessage = onMessage;
		}
		
		public HttpFlows(Optional<Flow> onMessage) {
			this.onMessage = onMessage.orElse(NoFlow.INSTANCE);
		}
		
		public Flow onMessage() {
			return onMessage;
		}
		
	}
	
	public static class SocketFlows {
		
		public static final SocketFlows NO_FLOWS = new SocketFlows(NoFlow.INSTANCE, NoFlow.INSTANCE, NoFlow.INSTANCE);
		
		private final Flow onConnect, onMessage, onDisconnect;
		
		public SocketFlows(Flow onConnect, Flow onMessage, Flow onDisconnect) {
			this.onConnect = onConnect;
			this.onMessage = onMessage;
			this.onDisconnect = onDisconnect;
		}
		
		public SocketFlows(Optional<Flow> onConnect, Optional<Flow> onMessage, Optional<Flow> onDisconnect) {
			this.onConnect = onConnect.orElse(NoFlow.INSTANCE);
			this.onMessage = onMessage.orElse(NoFlow.INSTANCE);
			this.onDisconnect = onDisconnect.orElse(NoFlow.INSTANCE);
		}
		
		public Flow onConnect() {
			return onConnect;
		}
		
		public Flow onMessage() {
			return onMessage;
		}
		
		public Flow onDisconnect() {
			return onDisconnect;
		}
		
	}
	
	public static class SafeChannelGroup {
		
		private final ChannelGroup channels;
		
		public SafeChannelGroup(ChannelGroup channels) {
			this.channels = channels;
		}
		
		public ChannelGroupFuture writeAndFlush(Object message, ChannelMatcher matcher) {
			return channels.writeAndFlush(message, matcher);
		}

		public ChannelGroupFuture write(Object message, ChannelMatcher matcher) {
			return channels.write(message, matcher);
		}

		public ChannelGroupFuture disconnect(ChannelMatcher matcher) {
			return channels.disconnect(matcher);
		}

		public ChannelGroupFuture close(ChannelMatcher matcher) {
			return channels.close(matcher);
		}
		
		public Stream<Channel> filter(Predicate<Channel> filter) {
			return channels.stream().filter(filter);
		}
		
	}

}
