package reka.net;

import static com.google.common.base.Preconditions.checkNotNull;
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
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.app.ApplicationComponent;
import reka.flow.Flow;
import reka.module.PortChecker;
import reka.net.ChannelAttrs.AttributeMatcher;
import reka.net.NetSettings.SslSettings;
import reka.net.NetSettings.Type;
import reka.net.http.HostAndPort;
import reka.net.http.server.HttpChannelSetup;
import reka.net.http.server.HttpInitializer;
import reka.net.http.server.HttpOrWebsocket;
import reka.net.http.server.WebsocketChannelSetup;
import reka.net.socket.SocketFlowHandler;
import reka.runtime.NoFlow;
import reka.util.AsyncShutdown;
import reka.util.Identity;

import com.google.common.collect.ImmutableMap;

public class NetManager {
	
	private static final Logger log = LoggerFactory.getLogger(NetManager.class);

	private final EventLoopGroup nettyEventGroup;
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
	private final Map<Identity,Map<NetSettings,Integer>> deployed = new HashMap<>();
	
	private final boolean epoll;
	
	private final Class<? extends ServerChannel> nettyServerChannelType;
	private final Class<? extends Channel> nettyClientChannelType;

	private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	public NetManager() {
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
	
	public ApplicationComponent deployHttp(Identity identity, HostAndPort listen, HttpFlows flows) {
		NetSettings settings = NetSettings.http(listen.port(), listen.host());
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.httpAdd(identity, settings.host().get(), flows));
	}
	
	public ApplicationComponent deployHttps(Identity identity, HostAndPort listen, SslSettings ssl, HttpFlows flows) {
		checkNotNull(ssl, "must pass in ssl settings for https");
		NetSettings settings = NetSettings.https(listen.port(), listen.host(), ssl);
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.httpAdd(identity, settings.host().get(), flows));
	}
	
	public ApplicationComponent deployWebsocket(Identity identity, HostAndPort listen, SocketFlows flows) {
		NetSettings settings = NetSettings.ws(listen.port(), listen.host());
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.websocketAdd(identity, settings.host().get(), flows));
	}
	
	public ApplicationComponent deployWebsocketSsl(Identity identity, HostAndPort listen, SslSettings ssl, SocketFlows flows) {
		NetSettings settings = NetSettings.wss(listen.port(), listen.host(), ssl);
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.websocketAdd(identity, settings.host().get(), flows));
	}
	
	public ApplicationComponent deploySocket(Identity identity, int port, SocketFlows flows) {
		NetSettings settings = NetSettings.socket(port);
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.socketSet(identity, flows));
	}
	
	public ApplicationComponent deploySocketSsl(Identity identity, int port, SslSettings ssl, SocketFlows flows) {
		NetSettings settings = NetSettings.socketSsl(port, ssl);
		PortHandler handler = ensurePortHandler(settings);
		int version = saveSettingsAndIncrementVersion(identity, settings);
		return new NetApplicationComponent(identity, settings, version, handler.socketSet(identity, flows));
	}
	
	public class NetApplicationComponent implements ApplicationComponent {

		private final Identity identity;
		private final NetSettings settings;
		private final int version;
		private final Runnable remove;
		
		public NetApplicationComponent(Identity identity, NetSettings settings, int version, Runnable remove) {
			this.identity = identity;
			this.settings = settings;
			this.version = version;
			this.remove = remove;
		}
		
		@Override
		public void undeploy() {
			PortHandler handler = handlers.get(settings.port());
			if (handler == null) return;
			Map<NetSettings, Integer> m = deployed.get(identity);
			if (m == null) return;
			if (!m.containsKey(settings)) return;
			int currentVersion = m.get(settings);
			if (version != currentVersion) {
				log.info("not undeploying as the version has incremented {} -> {}", version, currentVersion);
				return;
			}
			remove.run();
			if (handler.isEmpty()) {
				handlers.remove(settings.port());
				handler.shutdownAndWait();
			}
			m.remove(settings);
			if (m.isEmpty()) {
				deployed.remove(identity);
			}
		}

		@Override
		public Runnable pause() {
			PortHandler handler = handlers.get(settings.port());
			if (handler == null) return () -> {};
			switch (settings.type()) {
			case HTTP:
				return handler.httpPause(settings.host().get());
			case WEBSOCKET:
				return handler.websocketPause(settings.host().get());
			case SOCKET:
				return handler.socketPause();
			default:
				return () -> {};
			}
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
		
		private final HttpChannelSetup http;
		private final WebsocketChannelSetup websocket;
		
		HttpPortHandler(int port, SslSettings sslSettings) {
			super(port, sslSettings);
			http = new HttpChannelSetup(channels, port, sslSettings != null);
			websocket = new WebsocketChannelSetup(channels, port);
			initializer = new HttpInitializer(new HttpOrWebsocket(http, websocket), sslSettings);
		}

		@Override
		protected ChannelInitializer<SocketChannel> initializer() {
			return initializer;
		}

		@Override
		public boolean supports(Type type) {
			return type == Type.HTTP || type == Type.WEBSOCKET;
		}

		@Override
		public Runnable httpAdd(Identity identity, String host, HttpFlows flows) {
			Runnable undeploy = http.add(host, identity, flows);
			start();
			return undeploy;
		}

		@Override
		public Runnable websocketAdd(Identity identity, String host, SocketFlows flows) {
			Runnable undeploy = websocket.add(host, identity, flows);
			start();
			return undeploy;
		}
		
		@Override
		public Runnable httpPause(String host) {
			return http.pause(host);
		}

		@Override
		public Runnable websocketPause(String host) {
			return websocket.pause(host);
		}
		
		@Override
		public Runnable socketPause() {
			throw unsupported("this is http/websocket not socket");
		}
		
		@Override
		public boolean isEmpty() {
			return http.isEmpty() && websocket.isEmpty();
		}

		@Override
		public Runnable socketSet(Identity identity, SocketFlows flows) {
			throw runtime("we are doing http on this port, undeploy this first before using it for sockets. sockets don't have a host so they can't co-exist with http.");
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
		public Runnable socketSet(Identity identity, SocketFlows flows) {
			isSet = true;
			socketHandler.setFlows(flows);
			start();
			return () -> socketUnset(identity, flows);
		}
		
		private void socketUnset(Identity identity, SocketFlows flows) {
			if (socketHandler.unsetFlows(flows)) {
				isSet = false;
			}
		}

		@Override
		public boolean supports(Type type) {
			return type == Type.SOCKET;
		}

		@Override
		public Runnable httpAdd(Identity identity, String host, HttpFlows flows) {
			throw unsupported("this port is being used for a socket and you cannot do http on it at the same time");
		}

		@Override
		public Runnable websocketAdd(Identity identity, String host, SocketFlows flows) {
			throw unsupported("this port is being used for a socket and you cannot do websockets on it at the same time");
		}

		@Override
		public Runnable httpPause(String host) {
			throw unsupported("this is socket not http/websocket");
		}

		@Override
		public Runnable websocketPause(String host) {
			throw unsupported("this is socket not http/websocket");
		}
		
		@Override
		public Runnable socketPause() {
			log.warn("socket pause is not supported yet");
			return () -> {};
		}

		@Override
		public boolean isEmpty() {
			return !isSet;
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
		
		public abstract boolean supports(Type type);
		public abstract boolean isEmpty();
		
		public abstract Runnable httpAdd(Identity identity, String host, HttpFlows flows);
		public abstract Runnable websocketAdd(Identity identity, String host, SocketFlows flows);
		public abstract Runnable socketSet(Identity identity, SocketFlows flows);
		
		public abstract Runnable httpPause(String host);
		public abstract Runnable websocketPause(String host);
		public abstract Runnable socketPause();
		
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
				
				 // channel initializers need to turn this back on if they rely on it
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
		
	}
	
	public Map<Identity,Map<NetSettings,Integer>> deployed() {
		return ImmutableMap.copyOf(deployed);
	}
	
	public ChannelGroupWithMatcher channels(ChannelMatcher matcher) {
		return new ChannelGroupWithMatcher(channels, matcher);
	}
	
	public ChannelGroupWithMatcher channels(Identity identity) {
		return new ChannelGroupWithMatcher(channels, new AttributeMatcher<>(ChannelAttrs.identity, identity));
	}
	
	public boolean isAvailable(Identity identity, HostAndPort listen) {
		return deployed.entrySet().stream().allMatch(e -> {
			Identity deployedIdentity = e.getKey();
			if (deployedIdentity.equals(identity)) return true;
			Set<NetSettings> s = e.getValue().keySet();
			return s.stream().allMatch(settings -> {
				
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
			
		});
	}
	
	public boolean isAvailable(Identity identity, int port) {
		return deployed.entrySet().stream().allMatch(e -> {
			Identity deployedIdentity = e.getKey();
			if (deployedIdentity.equals(identity)) return true;
			Set<NetSettings> s = e.getValue().keySet();
			return s.stream().allMatch(settings -> settings.port() != port);
		});
	}

	public class Undeploy implements Runnable {
		
		private final Identity identity;
		private final NetSettings settings;
		private final int version;
		private final Runnable remove;
		
		public Undeploy(Identity identity, NetSettings settings, int version, Runnable remove) {
			this.identity = identity;
			this.settings = settings;
			this.version = version;
			this.remove = remove;
		}

		@Override
		public void run() {
			PortHandler handler = handlers.get(settings.port());
			if (handler == null) return;
			Map<NetSettings, Integer> m = deployed.get(identity);
			if (m == null) return;
			if (!m.containsKey(settings)) return;
			int currentVersion = m.get(settings);
			if (version != currentVersion) {
				log.info("not undeploying as the version has incremented {} -> {}", version, currentVersion);
				return;
			}
			remove.run();
			if (handler.isEmpty()) {
				handlers.remove(settings.port());
				handler.shutdownAndWait();
			}
		}
		
	}
	
	private PortHandler ensurePortHandler(NetSettings settings) {
		PortHandler portHandler = handlers.get(settings.port());
		if (portHandler != null) {
			if (!portHandler.supports(settings.type())) {
				throw runtime("cannot deploy %s to %s", settings.type(), portHandler.getClass());
			}
			if (!Objects.equals(portHandler.sslSettings(), settings.sslSettings())) {
				throw runtime("must have identical ssl settings on same port (SNI is not supported yet)");
			}
 		} else {
 			if (settings.type() == Type.SOCKET) {
				portHandler = new SocketPortHandler(settings.port(), settings.sslSettings());
			} else {
				portHandler = new HttpPortHandler(settings.port(), settings.sslSettings());
			}
 			handlers.put(settings.port(), portHandler);
 		}
		return portHandler;
	}

	private int saveSettingsAndIncrementVersion(Identity identity, NetSettings settings) {
		deployed.computeIfAbsent(identity, unused -> new HashMap<>());
		return deployed.get(identity).compute(settings, (unused, v) -> v != null ? v + 1 : 1);
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
		public boolean check(Identity identity, int port, Optional<String> host) {
			if (host.isPresent()) {
				return isAvailable(identity, new HostAndPort(host.get(), port));
			} else {
				return isAvailable(identity, port);
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
	
	public static class ChannelGroupWithMatcher implements Iterable<Channel> {

		private final ChannelGroup channels;
		private final ChannelMatcher base;
		
		public ChannelGroupWithMatcher(ChannelGroup channels, ChannelMatcher matcher) {
			this.channels = channels;
			this.base = matcher;
		}
		
		public ChannelGroupFuture writeAndFlush(Object message) {
			return channels.writeAndFlush(message, base);
		}

		public ChannelGroupFuture write(Object message) {
			return channels.write(message, base);
		}

		public ChannelGroupFuture disconnect() {
			return channels.disconnect(base);
		}

		public ChannelGroupFuture close() {
			return channels.close(base);
		}
		
		public ChannelGroupWithMatcher and(ChannelMatcher matcher) {
			if (base == matcher) return this;
			return new ChannelGroupWithMatcher(channels, ChannelMatchers.compose(base, matcher));
		}
		
		public <T> ChannelGroupWithMatcher withAttr(AttributeKey<T> key, T value) {
			return and(new AttributeMatcher<>(key, value));
		}
		
		public long count() {
			return channels.stream().filter(c -> base.matches(c)).count();
		}

		@Override
		public Iterator<Channel> iterator() {
			return channels.stream().filter(c -> base.matches(c)).iterator();
		}
		
	}

}
