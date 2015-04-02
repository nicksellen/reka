package reka.exec.ssh;

import static java.lang.String.format;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Message;
import net.schmizz.sshj.common.SSHPacket;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.ChannelOutputStream;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.Transport;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.exec.ExecConfigurer.ExecScripts;
import reka.exec.SshConfig;

public class RekaSshClient {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Object lock = new Object();
	
	private final AtomicInteger version = new AtomicInteger(1);
	private final byte[] sha1;
	private final int timeoutSeconds;
	private final List<Runnable> beforeDisconnect = new ArrayList<>();
	
	private final SshConfig config;
	
	private volatile SSHClient client;
	
	public RekaSshClient(SshConfig config, int timeoutSeconds) {
		this.config = config;
		this.sha1 = config.sha1();
		this.timeoutSeconds = timeoutSeconds;
	}
	
	public SSHClient connect() throws IOException {
		synchronized (lock) {
			if (client != null && client.isConnected()) {
				return client;
			}
			if (client != null) {
				try {
					client.disconnect();
				} catch (Throwable t) {
					// whatever...
					log.warn("exception while disconnecting previous client", t);
				}
			}
			client = new SSHClient();
			
			client.addHostKeyVerifier(config.hostkey());
			client.useCompression();

			log.info("connecting to {}@{}:{}", config.user(), config.hostname(), config.port());
			client.connect(config.hostname(), config.port());
			
			KeyProvider keyProvider = client.loadKeys(config.privateKeyAsString(), 
													  config.publicKeyAsString(), 
												      PasswordUtils.createOneOff(config.passphrase()));
			
			client.authPublickey(config.user(), keyProvider);
			
			client.getConnection().getKeepAlive().setKeepAliveInterval(30);
			
			return client;
		}
	}
	
	private SSHClient client() {
		synchronized (lock) {
			int attempts = 3;
			Throwable t = null;
			for (int i = 0; i < attempts; i++) {
				try {
					return connect();
				} catch (Throwable e) {
					t = e;
				}
			}
			log.error("failed to connect after " + attempts + " attempts", t);
			throw runtime("failed to connect after %s attempts: %s", attempts, t.getMessage());
		}
	}
	
	public void onBeforeDisconnect(Runnable runnable) {
		beforeDisconnect.add(runnable);
	}
	
	public int version() {
		return version.get();
	}
	
	public void version(int value) {
		version.set(value);
	}
	
	public byte[] sha1() {
		return sha1;
	}

	public String run(String command) {
		try {
			return exec(command, Collections.emptyMap()).get().out;
		} catch (InterruptedException | ExecutionException e) {
			throw unchecked(e);
		}
	}
	
	public CompletableFuture<ExecResult> exec(String command, Map<String,String> env) {
		
		CompletableFuture<ExecResult> future = new CompletableFuture<>();

		Reka.SharedExecutors.general.execute(() -> {
		
			ScheduledFuture<?> timeout = null;
			
			try {
			
				final AtomicReference<Session> sessionRef = new AtomicReference<>();
				final AtomicReference<Command> commandRef = new AtomicReference<>();
				
				timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
					try {
						Command cmd = commandRef.get();
						if (cmd != null) {
							cmd.close();	
						}
						Session session = sessionRef.get();
						if (session != null) {
							session.close();
						}
					} catch (Throwable t) {
						log.error("error closing channel", t);
					}
					future.completeExceptionally(runtime("timed out after %ds", timeoutSeconds));
				}, timeoutSeconds, TimeUnit.SECONDS);
				
				final Session session;
				
				synchronized (lock) {
					session = client().startSession();
					sessionRef.set(session);
				}
				
		        try {
		        	
		        	final Command cmd = session.exec(command);
		        	commandRef.set(cmd);
		        
		        	OutputStream in = cmd.getOutputStream();
		        	InputStream err = cmd.getErrorStream();
					InputStream out = cmd.getInputStream();
					
					for (Entry<String, String> e : env.entrySet()) {
						in.write((e.getKey() + "=\"" + e.getValue() + "\";\n").getBytes(StandardCharsets.UTF_8));
					}
					
					in.flush();
					writeEOF(in);
					in.close();
					
					ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
					ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
					
					while (cmd.getExitStatus() == null || err.available() > 0 || out.available() > 0) {
						if (err.available() > 0) {
							errBytes.write(err.read());
						}
						if (out.available() > 0) {
							outBytes.write(out.read());
						}
					}
					
					if (!timeout.isDone()) {
		
						timeout.cancel(true);
						
						future.complete(new ExecResult(
								cmd.getExitStatus(), 
								new String(outBytes.toByteArray(), StandardCharsets.UTF_8), 
								new String(errBytes.toByteArray(), StandardCharsets.UTF_8)));
		
					}
					
		        } finally {
		            session.close();
		        }
		        
			} catch (ConnectionException | SocketException e) {
				if (timeout != null) {
					timeout.cancel(true);
				}
				try {
					// reconnect?
					log.info("reconnecting to {}:{}", config.hostname(), config.port());
					connect();
					exec(command, env).whenComplete((res,t) -> {
						if (t != null) {
							future.completeExceptionally(t);
						} else {
							future.complete(res);
						}
					});
					
				} catch (Exception e1) {
					future.completeExceptionally(e1);
				}
				
			} catch (Throwable t) {
				if (timeout != null) {
					timeout.cancel(true);
				}
				future.completeExceptionally(t);
			}

		});
        
        return future;
	}

	public String[] sendScripts(ExecScripts scripts) {
		
		String tmpdirStr = run("mktemp -d").trim();
		java.nio.file.Path tmpdir = Paths.get(tmpdirStr);
		
		onBeforeDisconnect(() -> {
			run(format("rm -r \"%s\"", tmpdirStr));
		});
		
		try {
			
			java.nio.file.Path wrapperPath = tmpdir.resolve("__wrapper__");
			java.nio.file.Path scriptPath = tmpdir.resolve("__main__");
			SCPFileTransfer scp = client().newSCPFileTransfer();
			
			CountDownLatch latch = new CountDownLatch(scripts.extraScripts().size() + 2);
			
			Reka.SharedExecutors.general.execute(() -> {
				
				String wrapperSrc = "#!/bin/sh\nset -a\n. /dev/stdin\n" + scriptPath;
				
				try {
					scp.upload(new ByteBufferSourceFile(wrapperPath.getFileName().toString(), ByteBuffer.wrap(wrapperSrc.getBytes(StandardCharsets.UTF_8))), wrapperPath.toString());
					run("chmod +x " + wrapperPath);
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
					throw unchecked(e);
				}
			});
			
			Reka.SharedExecutors.general.execute(() -> {
				try {
					scp.upload(new ByteBufferSourceFile(scriptPath.getFileName().toString(), scripts.script()), scriptPath.toString());
					run("chmod +x " + scriptPath);
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
					throw unchecked(e);
				}
			});
			
			scripts.extraScripts().forEach((path, buffer) -> {

				Reka.SharedExecutors.general.execute(() -> {
					try {
						scp.upload(new ByteBufferSourceFile(path.getFileName().toString(), buffer), tmpdir.resolve(path).toString());
						latch.countDown();
					} catch (Exception e) {
						e.printStackTrace();
						throw unchecked(e);
					}
				});
				
			});
		
			if (latch.await(10, TimeUnit.SECONDS)) {
				return new String[]{ wrapperPath.toString() };
			} else {
				throw runtime("failed to send files after 10 seconds");
			}
			
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
		
	}
	
	public void disconnect() throws IOException {
		synchronized (lock) {
			if (client != null) {
				cleanup(); // TODO: we probably need to reconnect just to run these...
				client.disconnect();
				client = null;
			}
		}
	}

	private void cleanup() {
		beforeDisconnect.forEach(runnable -> {
			try {
				runnable.run();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		});
	}
	

	
	public static class ExecResult {
		
		public final int status;
		public final String out, err;
		
		public ExecResult(int status, String out, String err) {
			this.status = status;
			this.out = out;
			this.err = err;
		}
		
	}
	
	public static class ByteBufferSourceFile extends InMemorySourceFile {
		
		private final String name;
		private final long length;
		private final ByteBuffer buffer;
		
		public ByteBufferSourceFile(String name, ByteBuffer buffer) {
			this.name = name;
			this.length = buffer.limit();
			this.buffer = buffer;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public long getLength() {
			return length;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteBufferBackedInputStream(buffer);
		}
	}
	
	public static class ByteBufferBackedInputStream extends InputStream {

	    private final ByteBuffer buf;

	    public ByteBufferBackedInputStream(ByteBuffer buf) {
	        this.buf = buf;
	    }

	    public int read() throws IOException {
	        if (!buf.hasRemaining()) {
	            return -1;
	        }
	        return buf.get() & 0xFF;
	    }

	    public int read(byte[] bytes, int off, int len)
	            throws IOException {
	        if (!buf.hasRemaining()) {
	            return -1;
	        }

	        len = Math.min(len, buf.remaining());
	        buf.get(bytes, off, len);
	        return len;
	    }
	}
	
	private static void writeEOF(OutputStream o) {
		if (!(o instanceof ChannelOutputStream)) return;
		ChannelOutputStream out = (ChannelOutputStream) o;
		
		// https://github.com/hierynomus/sshj/issues/143
		
		try {
		
			Field f1 = ChannelOutputStream.class.getDeclaredField("trans");
			f1.setAccessible(true);
			Transport trans = (Transport) f1.get(out);
			
			Field f2 = ChannelOutputStream.class.getDeclaredField("chan");
			f2.setAccessible(true);
			Channel chan = (Channel) f2.get(out);
			
			trans.write(new SSHPacket(Message.CHANNEL_EOF).putUInt32(chan.getRecipient()));
		
		} catch (Exception t) {
			throw unchecked(t);
		}
		
	}

	
}