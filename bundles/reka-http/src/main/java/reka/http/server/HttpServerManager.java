package reka.http.server;

import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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

import reka.api.flow.Flow;
import reka.http.server.HttpSettings.SslSettings;
import reka.http.websockets.WebsocketHandler;

import com.google.common.collect.ImmutableMap;

public class HttpServerManager {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final Logger logger = LoggerFactory.getLogger(HttpServerManager.class.getSimpleName());

	private final NioEventLoopGroup group = new NioEventLoopGroup();
	
	private final Map<Integer,PortHandler> handlers = new HashMap<>();
		
	private final Object lock = new Object();
	private final Map<String,HttpSettings> deployed = new HashMap<>();
	
	public NioEventLoopGroup group() {
		return group;
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
				initializer = new HttpsHandler(httpOrWebsocketHandler, sslSettings.certChainFile(), sslSettings.keyFile());
			} else {
				initializer = new HttpInitializer(httpOrWebsocketHandler);
			}
		}
		
		public boolean isEmpty() {
			return httpHandler.isEmpty();
		}
		
		public PortHandler addHttp(String host, Flow flow) {
			httpHandler.add(host, flow);
			if (channel == null) start();
			return this;
		}


		public void pause(String host) {
			httpHandler.pause(host);
		}


		public void resume(String host) {
			httpHandler.resume(host);
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
		
		public boolean isSsl() {
			return sslSettings != null;
		}
		
		public SslSettings sslSettings() {
			return sslSettings;
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
					throw unchecked(e);
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
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childHandler(initializer);

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
			
			handler.accept(portHandler);
			
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
	
	public void undeploy(String identity, int undeployVersion) {
		log.debug("undeploying [{}]", identity);
		
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
				handler.remove(settings.host());
				if (handler.isEmpty()) {
					handlers.remove(settings.port());
				}
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
				handler.pause(settings.host());
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
				handler.resume(settings.host());
			}
			
		}
	}

}
