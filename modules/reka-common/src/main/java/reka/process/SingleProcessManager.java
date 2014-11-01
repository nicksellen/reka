package reka.process;

import static java.lang.String.format;
import static reka.util.Util.createEntry;
import static reka.util.Util.runtime;
import static reka.util.Util.unchecked;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;

public final class SingleProcessManager implements ProcessManager {

	private static final Logger log = LoggerFactory.getLogger(SingleProcessManager.class);

	private static final byte[] NEW_LINE = "\n".getBytes(StandardCharsets.UTF_8);
	private static final Class<?> unixProcessClass;
	
	static {
		Class<?> clazz = null;
		try {
			clazz = Class.forName("java.lang.UNIXProcess");
		} catch (ClassNotFoundException e) {
			// oh well
			e.printStackTrace();
		}
		unixProcessClass = clazz;
	}
	
	@SuppressWarnings("unused")
	private final ProcessBuilder builder;
	private final Process process;
	
	private final int pid;
	
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
	
	public SingleProcessManager(ProcessBuilder builder, boolean noreply) {
		this(builder, noreply, new LinkedBlockingDeque<>());
	}
	
	private final Object lock = new Object();
	
	protected SingleProcessManager(
			ProcessBuilder builder, 
			boolean noreply,
			BlockingDeque<Entry<String,Consumer<String>>> q) {
		this.q = q;
		this.builder = builder;
		this.noreply = noreply;
		
		try {
			process = builder.start();
		} catch (IOException e1) {
			throw unchecked(e1);
		}
		
		pid = tryAndGetPid();
		
		stdin = process.getOutputStream();
		stdout = process.getInputStream();
		stderr = process.getErrorStream();
		
		stdoutReader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));
		
		writerThread = new Thread() {
			
			@Override
			public void run() {
				try {
					while (process.isAlive() && !Thread.interrupted()) {
						Entry<String, Consumer<String>> entry = q.take();
						synchronized (lock) {
							String input = entry.getKey();
							byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
							stdin.write(bytes);
							stdin.write(NEW_LINE);
							stdin.flush();
							if (!noreply) consumerq.offer(entry.getValue());
						}
					}
				} catch (InterruptedException | IOException e) {
					terminate();
				}
			}
			
		};
		
		readerThread = new Thread() {
			
			@Override
			public void run() {
				try {
					Consumer<String> consumer = null;
					while (process.isAlive() && !Thread.interrupted()) {
						String output = stdoutReader.readLine();
						if (output != null) {
							synchronized (lock) { }
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
					// we can't talk to the process any more
				} finally {
					terminate();
					writerThread.interrupt();
				}
			}
		};

		
		if (pid != -1) {
			writerThread.setName(format("process-writer-%s", pid));
			readerThread.setName(format("process-reader-%s", pid));
		} else {
			writerThread.setName(format("process-writer"));
			readerThread.setName(format("process-reader"));
		}
	}
	
	private int tryAndGetPid() {
		int tryPid = -1;
		if (unixProcessClass != null && unixProcessClass.isInstance(process)) {
			try {
				Field field = unixProcessClass.getDeclaredField("pid");
				boolean original = field.isAccessible();
				field.setAccessible(true);
				tryPid = (int) field.get(process);
				field.setAccessible(original);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				// oh well
				e.printStackTrace();
				tryPid = -1;
			}
		}
		return tryPid;
	}

	@Override
	public void send(String input, Consumer<String> consumer) {
		if (!process.isAlive()) throw runtime("process is dead!");
		q.offer(createEntry(input, consumer));
	}

	@Override
	public void shutdown() {
		terminate();
	}

	@Override
	public void send(String input) {
		send(input, null);
	}

	@Override
	public void addListener(Consumer<String> consumer) {
		lineTriggers.add(consumer);
	}

	@Override
	public boolean up() {
		return process.isAlive();
	}
	
	@Override
	public void statusData(MutableData data) {
		if (pid != -1) {
			data.putInt("pid", pid);
		}
		data.putInt(Q_PATH, q.size());
		data.putInt("consumer-q", consumerq.size());
	}
	
	private static final int WAIT_SECONDS = 5;
	
	private static final long Q_DRAIN_WAIT = WAIT_SECONDS * 1000000000L;// in nanos
	
	private void terminate() {
		if (process == null) return;
		if (process.isAlive()) {
			
			long waitUntil = System.nanoTime() + Q_DRAIN_WAIT;

			if (!q.isEmpty()) {
				System.out.printf("waiting for q to empty %s\n", q.size());
				while (!q.isEmpty() && (System.nanoTime() < waitUntil)) {
					// wait
				}
				if (!q.isEmpty()) {
					log.warn("discarding {} things from q\n", q.size());
				}
				
			}
			
			if (!consumerq.isEmpty()) {
				System.out.printf("waiting for consumerq to empty %s\n", consumerq.size());
				while (!consumerq.isEmpty() && (System.nanoTime() < waitUntil)) {
					// wait
				}
				if (!consumerq.isEmpty()) {
					log.warn("discarding {} things from consumerq\n", consumerq.size());
				}
			}
			
			process.destroy();
			
			try {
				if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
					process.destroyForcibly();
					if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
						log.error("failed to terminate process");
					}
				}
			} catch (InterruptedException e) {
				// whatever
			}
		}
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
				System.err.printf("stderr: %s\n", new String(baos.toByteArray(), StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			// ignore
		}
	}

	@Override
	public void start() {
		writerThread.start();
		readerThread.start();
	}
	
}