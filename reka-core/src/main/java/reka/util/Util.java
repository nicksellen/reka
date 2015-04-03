package reka.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.codehaus.jackson.JsonGenerator;

import reka.config.Source;
import reka.config.SourceLinenumbers;
import reka.config.configurer.Configurer.InvalidConfigurationException;

import com.google.common.io.BaseEncoding;

public class Util {
	
	public static class UncheckedException extends RuntimeException {
		private static final long serialVersionUID = 2047845565258190433L;
		public UncheckedException(Throwable t) {
			super(t);
		}
		public UncheckedException(String msg, Throwable t) {
			super(msg, t);
		}
	}
	
	public static void printStackTrace() {
		try {
			throw runtime();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	public static void printf(String msg, Object... objs) {
		System.out.printf(msg, objs);
	}
	
	public static void println(String msg, Object... objs) {
		System.out.printf(msg + "\n", objs);
	}
	
	public static RuntimeException unchecked(Throwable t) {
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		} else {
			return new UncheckedException(t);
		}
	}
	
	public static Throwable unwrap(Throwable t) {
		Throwable result = t;
		while (result instanceof UncheckedException) {
			result = result.getCause();
		}
		return result;
	}

	public static RuntimeException unchecked(Throwable e, String msg, Object... args) {
		return new UncheckedException(format(msg, args), e);
	}
	
	public static NullPointerException nullPointerException(String field) {
		return new NullPointerException(field);
	}
	
	public static UnsupportedOperationException unsupported() {
		return new UnsupportedOperationException();
	}
	
	public static UnsupportedOperationException unsupported(String msg, Object... args) {
		return new UnsupportedOperationException(format(msg, args));
	}
	
	public static RuntimeException runtime() {
		return new RuntimeException();
	}
	
	public static RuntimeException runtime(String msg, Object... args) {
		if (args.length > 0) {
			msg = format(msg, args);
		}
		return new RuntimeException(msg);
	}
	
	public static int[] removedValues(int[] from, int[] to) {
		List<Integer> removed = new ArrayList<>();
		for (int existing : from) {
			boolean wasRemoved = true;
			for (int current : to) {
				if (existing == current) {
					wasRemoved = false;
					break;
				}
			}
			if (wasRemoved) {
				removed.add(existing);
			}
		}
		int[] out = new int[removed.size()];
		for (int i = 0; i < removed.size(); i++) out[i] = removed.get(i);
		return out;
		
	}
	
	public static <K,V> Entry<K, V> createEntry(K key, V value) {
		return new AbstractMap.SimpleEntry<K,V>(key, value);
	}
	
	public static <T> CompletableFuture<T> safelyCompletable(CompletableFuture<T> future, ThrowingRunnable runnable) {
		try {
			runnable.run();
		} catch (Throwable t) {
			if (!future.isDone()) {
				future.completeExceptionally(t);
			}
		}
		return future;
	}
	
	public static <T> CompletableFuture<T> safelyCompletable(ThrowingConsumer<CompletableFuture<T>> consumer) {
    	CompletableFuture<T> future = new CompletableFuture<>();
		return safelyCompletable(future, () -> {
			consumer.accept(future);
		});
	}
		
	public static void ignoreExceptions(ThrowingRunnable r) {
		try {
			r.run();
		} catch (Throwable t) {
			// ignore
		}
	}
	
	public static class InvalidConfigurationExceptionJsonProvider implements JsonProvider {
		
		private final InvalidConfigurationException ex;
		
		public InvalidConfigurationExceptionJsonProvider(InvalidConfigurationException ex) {
			this.ex = ex;
		}
		
		@Override
		public void writeJsonTo(JsonGenerator json) throws IOException {
			json.writeStartArray();
			ex.errors().forEach(e -> {
				try {
					json.writeStartObject();
					json.writeStringField("message", e.message());
					Source source = e.config().source();
					SourceLinenumbers linenumbers = source.linenumbers();
					if (linenumbers != null) {
							json.writeFieldName("linenumbers");
							json.writeStartObject();
							json.writeNumberField("start-line", linenumbers.startLine());
							json.writeNumberField("end-line", linenumbers.endLine());
							json.writeNumberField("start-pos", linenumbers.startPos());
							json.writeNumberField("end-pos", linenumbers.endPos());
							json.writeEndObject();
					}
					json.writeEndObject();
				} catch (Exception e1) {
					throw unchecked(e1);
				}
			});
			json.writeEndArray();
		}
	}

	private static final Encoder BASE64_ENCODER = Base64.getEncoder();
	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final BaseEncoding BASE32 = BaseEncoding.base32().omitPadding().lowerCase();
	
	public static String encode64(String val) {
		return BASE64_ENCODER.encodeToString(val.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String decode64(String val) {
		return new String(BASE64_DECODER.decode(val), StandardCharsets.UTF_8);
	}
	
	public static String encode32(String val) {
		return BASE32.encode(val.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String decode32(String val) {
		return new String(BASE32.decode(val), StandardCharsets.UTF_8);
	}
	
	public static void deleteRecursively(java.nio.file.Path path) {
		if (!Files.exists(path)) return;
		try {
			Files.walkFileTree(path, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					if (exc != null) throw exc;
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (exc != null) throw exc;
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				
			});
		} catch (IOException e) {
			throw unchecked(e);
		}
	}

	public static void unzip(byte[] bytes, java.nio.file.Path dest) {
		try {

			ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				java.nio.file.Path filepath = dest.resolve(e.getName());
				Files.createDirectories(filepath.getParent());
				FileOutputStream out = new FileOutputStream(filepath.toFile());
				try {
					byte[] buf = new byte[8192];
					int len;
					while ((len = zip.read(buf, 0, buf.length)) > 0) {
						out.write(buf, 0, len);
					}
				} finally {
					ignoreExceptions(() -> out.close());
					ignoreExceptions(() -> zip.closeEntry());
				}
			}
		} catch (Throwable t) {
			throw unchecked(t);
		}
	}

	public static String hex(byte[] b) {
	  StringBuilder result = new StringBuilder();
	  for (int i = 0; i < b.length; i++) {
	    result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
	  }
	  return result.toString();
	}
	
	public static String sha1hex(byte[] bs) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.reset();
			sha1.update(bs);
			return hex(sha1.digest());
		} catch (NoSuchAlgorithmException e) {
			throw unchecked(e);
		}
	}
	
	public static boolean isSingleThreaded(ExecutorService executor) {
		return countThreads(executor, 1000) == 1;
	}
	
	public static int countThreads(ExecutorService executor, int n) {
		Map<Long,Boolean> threadIds = new ConcurrentHashMap<>();
		CountDownLatch latch = new CountDownLatch(n);
		for (int i = 0; i < n; i++) {
			executor.execute(() -> {
				threadIds.put(Thread.currentThread().getId(), true);
				latch.countDown();
			});
		}
		try {
			latch.await();
			return threadIds.size();
		} catch (InterruptedException e) {
			throw unchecked(e);
		}
	}

	public static String rootExceptionMessage(Throwable t) {
		Collection<String> msgs = allExceptionMessages(t);
		return msgs.isEmpty() ? "unknown" : msgs.iterator().next();
	}
	
	public static Throwable rootCause(Throwable t) {
		Throwable cause = t.getCause();
		while (cause != null) {
			t = cause;
			cause = t.getCause();
		}
		return t;
	}
	
	public static String allExceptionMessages(Throwable t, String joiner) {
		return allExceptionMessages(t).stream().collect(joining(joiner));
	}
	
	private static Collection<String> allExceptionMessages(Throwable tOriginal) {
		List<String> result = new ArrayList<>();
		
		Throwable t = tOriginal;
		
		while (t != null) {
			if (t.getMessage() != null) {
				result.add(t.getMessage());
			} else {
				result.add(t.getClass().getName());
			}
			t = t.getCause();
		}
		
		Collections.reverse(result);
		
		if (result.isEmpty()) {
			result.add("unknown error");
		}
		
		return result;
	}
	
	private static volatile Thread keepAliveThread;
	
	public static void startKeepAliveThread() {
		if (keepAliveThread != null) return;
		
		keepAliveThread = new Thread() {
			@Override
			public void run() {
				for (;;) {
					try {
						Thread.sleep(Long.MAX_VALUE);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		};
		keepAliveThread.setDaemon(false);
		keepAliveThread.setName("reka-keep-alive");
		keepAliveThread.setPriority(Thread.MIN_PRIORITY);
		keepAliveThread.start();
	}
	
}
