package reka.process;

import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

public class SingleProcessManager implements ProcessManager {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@SuppressWarnings("unused")
	private final ProcessBuilder builder;
	private final Process process;

	@SuppressWarnings("unused")
	private final boolean noreply;
	
	private final OutputStream stdin;
	private final BufferedReader stdoutReader;
	private final InputStream stdout;
	private final InputStream stderr;
	
	private final Thread readerThread, writerThread;
	
	private final BlockingDeque<Entry<String,Consumer<String>>> q;
	
	private final Deque<Consumer<String>> consumerq = new ArrayDeque<>();
	
	private final List<Consumer<String>> lineTriggers = Collections.synchronizedList(new ArrayList<>());
	
	public SingleProcessManager(
			ProcessBuilder builder, 
			BlockingDeque<Entry<String,Consumer<String>>> q, 
			boolean noreply) {
		this.q = q;
		this.builder = builder;
		this.noreply = noreply;
		try {
			this.process = builder.start();
		} catch (IOException e1) {
			throw unchecked(e1);
		}
		
		stdin = process.getOutputStream();
		stdout = process.getInputStream();
		stderr = process.getErrorStream();
		
		stdoutReader = new BufferedReader(new InputStreamReader(stdout, Charsets.UTF_8));
		
		writerThread = new Thread() {
			
			@Override
			public void run() {
				try {
					while (process.isAlive()) {
						Entry<String, Consumer<String>> entry = q.take();
						String input = entry.getKey();
						byte[] bytes = input.getBytes(Charsets.UTF_8);
						stdin.write(bytes);
						stdin.write(NEW_LINE);
						stdin.flush();
						if (!noreply) consumerq.offer(entry.getValue());
					}
				} catch (InterruptedException | IOException e) {
					throw unchecked(e);
				}
			}
			
		};
		
		writerThread.start();
		
		readerThread = new Thread() {
			
			@Override
			public void run() {
				try {
					Consumer<String> consumer = null;
					while (process.isAlive()) {
						String output = stdoutReader.readLine();
						if (output != null) {
							if (!noreply) consumer = consumerq.poll();
							if (consumer != null) {
								consumer.accept(output);
							} else if (lineTriggers.isEmpty()) {
								lineTriggers.forEach(c -> c.accept(output));
							}
						}
						drain(stderr);
					}	
				} catch (IOException e) {
					log.error("process thread stopped :(", e);
				}
			}
		};
		
		readerThread.start();
	}
	
	private void drain(InputStream stream) {
		byte[] buf = new byte[8192];
		int readLength;
		try {
			if (stream.available() > 0) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((readLength = stream.read(buf, 0, buf.length)) > 0) { 
					baos.write(buf, 0, readLength);
				}
				System.err.printf("stderr: %s\n", new String(baos.toByteArray(), Charsets.UTF_8));
			}
		} catch (IOException e) {
			e.printStackTrace();
			// ignore
		}
	}
	
	private static final byte[] NEW_LINE = "\n".getBytes(Charsets.UTF_8);
	
	@Override
	public void run(String input, Consumer<String> consumer) {
		if (!process.isAlive()) throw runtime("process is dead!");
		q.offer(createEntry(input, consumer));
	}

	@Override
	public void kill() {
		if (process.isAlive()) {
			process.destroyForcibly();
		}
	}

	@Override
	public void run(String input) {
		run(input, null);
	}

	@Override
	public void addLineTrigger(Consumer<String> consumer) {
		lineTriggers.add(consumer);
	}
	
}