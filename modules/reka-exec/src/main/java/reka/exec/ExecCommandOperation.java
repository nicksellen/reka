package reka.exec;

import static reka.util.Util.unchecked;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Reka;
import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;
import reka.exec.ExecConfigurer.ExecScripts;

public class ExecCommandOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String[] command;
	private final Path outInto, errInto, statusInto;
	private final int timeoutSeconds = 5;
	
	public ExecCommandOperation(ExecScripts scripts, java.nio.file.Path tmp, Path into) {

		this.command = writeScripts(scripts, tmp);
		
		this.outInto = into.add("out");
		this.errInto = into.add("err");
		this.statusInto = into.add("status");
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {
		
		ProcessBuilder builder = new ProcessBuilder();
		
		builder.command(command);
		Map<String, String> env = builder.environment();
		env.clear();
		data.forEachContent((path, content) -> {
			String key = path.join("__").toUpperCase().replaceAll("[^A-Z0-9]", "_");
			String val = content.toString();
			env.put(key, val);
		});
		
		Reka.SharedExecutors.general.execute(() -> {
		
			try {
			
				Process process = builder.start();
				
				ScheduledFuture<?> timeout = Reka.SharedExecutors.scheduled.schedule(() -> {
					try {
						process.destroyForcibly();
					} catch (Throwable t) {
						log.error("error destroying process", t);
					}
					res.error("timed out after %ds", timeoutSeconds);
				}, timeoutSeconds, TimeUnit.SECONDS);
				
				InputStream err = process.getErrorStream();
				InputStream out = process.getInputStream();
				
				ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
				ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
				
				while (process.isAlive() || err.available() > 0 || out.available() > 0) {
					if (err.available() > 0) {
						errBytes.write(err.read());
					}
					if (out.available() > 0) {
						outBytes.write(out.read());
					}
				}
				
				if (!timeout.isDone()) {

					timeout.cancel(true);
					
					data.putInt(statusInto, process.exitValue());
					data.putString(outInto, new String(outBytes.toByteArray(), StandardCharsets.UTF_8));
					data.putString(errInto, new String(errBytes.toByteArray(), StandardCharsets.UTF_8));

					res.done();

				}

			} catch (Throwable t) {
				res.error(t);
			}
		
		});
		
	}
	
	private static String[] writeScripts(ExecScripts scripts, java.nio.file.Path tmp) {
		try {
			java.nio.file.Path dir = Files.createTempDirectory(tmp, "exec");
			java.nio.file.Path scriptPath = dir.resolve("__main__");
			
			writeByteBufferTo(scriptPath, scripts.script());
			
			scripts.extraScripts().forEach((path, buf) -> {
				try {
					writeByteBufferTo(dir.resolve(path), buf);
				} catch (Exception e) {
					throw unchecked(e);
				}
			});
			File scriptFile = scriptPath.toFile();
			scriptFile.setExecutable(true, true);
			return new String[] { scriptFile.getAbsolutePath() };
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	private static void writeByteBufferTo(java.nio.file.Path path, ByteBuffer buf) throws IOException {
		// TODO: not sure if this is write
		FileChannel channel = new FileOutputStream(path.toFile()).getChannel();
		channel.write(buf);
		channel.close();
	}

}
