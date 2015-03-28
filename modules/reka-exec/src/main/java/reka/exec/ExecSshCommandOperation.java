package reka.exec;

import static java.lang.String.format;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;
import reka.exec.ExecConfigurer.ExecScripts;
import reka.exec.ExecConfigurer.RekaSSHClient;

public class ExecSshCommandOperation implements AsyncOperation {
	
	private static final Logger log = LoggerFactory.getLogger(ExecSshCommandOperation.class);

	private final String command;
	private final Path outInto, errInto, statusInto;
	private final int timeoutSeconds = 5;
	
	private final SSHClient ssh;
	
	public ExecSshCommandOperation(ExecScripts scripts, RekaSSHClient ssh, Path into) {
		
		this.outInto = into.add("out");
		this.errInto = into.add("err");
		this.statusInto = into.add("status");
		this.ssh = ssh;
		
		try {
			this.command = sendScripts(scripts, ssh);
		} catch (Throwable t) {
			t.printStackTrace();
			throw unchecked(t);
		}
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {

		Map<String,String> env = new HashMap<>();
		
		data.forEachContent((path, content) -> {
			String key = path.join("__").toUpperCase().replaceAll("[^A-Z0-9]", "_");
			String val = content.toString();
			env.put(key, val);
		});
		
		exec(command, env).whenComplete((result, ex) ->  {
			if (ex != null) {
				res.error(ex);
				return;
			}
			data.putInt(statusInto, result.status);
			data.putString(outInto, result.out);
			data.putString(errInto, result.err);
			res.done();
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
		
			try {
			
				final AtomicReference<Session> sessionRef = new AtomicReference<>();
				final AtomicReference<Command> commandRef = new AtomicReference<>();
				
				ScheduledFuture<?> timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
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
				
				synchronized (ssh) {
					session = ssh.startSession();
					sessionRef.set(session);
				}
				
		        try {
		        	
		        	/* this doesn't work. I think sshj is broken :(
		        	 
		        	for (Entry<String, String> e : env.entrySet()) {
		        		log.info("setting session env {} : {}", e.getKey(), e.getValue());
		        		session.setEnvVar(e.getKey(), e.getValue());
		        	}
		        	*/
		        	
		        	final Command cmd = session.exec(command);
		        	commandRef.set(cmd);
		        
		        	InputStream err = cmd.getErrorStream();
					InputStream out = cmd.getInputStream();
					
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
	        
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}

		});
        
        return future;
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
	
	
	private String sendScripts(ExecScripts scripts, RekaSSHClient ssh) {

		String tmpdirStr = run("mktemp -d").trim();
		java.nio.file.Path tmpdir = Paths.get(tmpdirStr);
		
		ssh.onBeforeDisconnect(() -> {
			run(format("rm -r \"%s\"", tmpdirStr));
		});
		
		try {
			
			String scriptPath = tmpdir.resolve("__main__").toString();
			SCPFileTransfer scp = ssh.newSCPFileTransfer();
			
			CountDownLatch latch = new CountDownLatch(scripts.extraScripts().size() + 1);
			
			Reka.SharedExecutors.general.execute(() -> {
				try {
					scp.upload(new ByteBufferSourceFile("__main__", scripts.script()), tmpdir.resolve("__main__").toString());
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
				return scriptPath;
			} else {
				throw runtime("failed to send files after 10 seconds");
			}
			
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
		
	}

}
