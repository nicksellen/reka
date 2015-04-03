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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.data.MutableData;

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
	
	private final ProcessBuilder builder;
	
	private volatile Process process;	
	private volatile int pid;
	
	private final boolean noreply;
	
	private final AtomicBoolean isShutdown = new AtomicBoolean(false);
	
	private final BlockingDeque<Entry<String,Consumer<String>>> q;
	
	private volatile Deque<Consumer<String>> activeconsumerq;
	
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
		if (isShutdown.compareAndSet(false, true)) {
			terminate(process, activeconsumerq, true);
		}
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
		data.putInt("consumer-q", activeconsumerq.size());
	}
	
	private static final int WAIT_SECONDS = 5;
	
	private static final long Q_DRAIN_WAIT = WAIT_SECONDS * 1000000000L;// in nanos
	
	private void terminate(Process process, Deque<Consumer<String>> consumerq, boolean waitForQueue) {
		if (process == null) return;
		if (process.isAlive()) {
			
			if (waitForQueue) {
			
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
					while (!activeconsumerq.isEmpty() && (System.nanoTime() < waitUntil)) {
						// wait
					}
					if (!consumerq.isEmpty()) {
						log.warn("discarding {} things from consumerq\n", consumerq.size());
					}
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

		try {
			process = builder.start();
		} catch (IOException e1) {
			throw unchecked(e1);
		}
		
		pid = tryAndGetPid();
		
		Deque<Consumer<String>> consumerq = new ArrayDeque<>();
		activeconsumerq = consumerq;
		
		OutputStream stdin = process.getOutputStream();
		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();
		
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));
		
		Thread writerThread = new Thread() {
			
			@Override
			public void run() {
				Entry<String, Consumer<String>> entry = null;
				try {
					while (process.isAlive() && !Thread.interrupted()) {
						entry = q.take();
						synchronized (lock) {
							String input = entry.getKey();
							byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
							stdin.write(bytes);
							stdin.write(NEW_LINE);
							stdin.flush();
							if (!noreply) consumerq.offer(entry.getValue());
							entry = null;
						}
					}
				} catch (InterruptedException | IOException e) {
					// ignore
				}			
				
				if (!isShutdown.get()) {
					if (entry != null) {
						q.offer(entry); // re-offer it, we failed to write to process
					}
					Process outgoingProcess = process;
					SingleProcessManager.this.start(); // respawn!
					terminate(outgoingProcess, consumerq, false);
				}
								
			}
			
		};
		
		Thread readerThread = new Thread() {
			
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
					writerThread.interrupt();
				}
				if (!consumerq.isEmpty()) {
					log.warn("discarding {} items from consumerq", consumerq.size());
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
		
		writerThread.start();
		readerThread.start();
	}
	
}