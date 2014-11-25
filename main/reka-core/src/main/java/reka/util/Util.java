package reka.util;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.codehaus.jackson.JsonGenerator;

import reka.api.JsonProvider;
import reka.config.Source;
import reka.config.SourceLinenumbers;
import reka.config.configurer.Configurer.InvalidConfigurationException;

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
		return new RuntimeException(format(msg, args));
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
	
	public static <T> CompletableFuture<T> safelyCompletable(ThrowingConsumer<CompletableFuture<T>> consumer) {
    	CompletableFuture<T> future = new CompletableFuture<>();
		try {
			consumer.accept(future);
		} catch (Throwable t) {
			if (!future.isDone()) {
				future.completeExceptionally(t);
			}
		}
		return future;
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
	
	public static String encode64(String val) {
		return BASE64_ENCODER.encodeToString(val.getBytes(StandardCharsets.UTF_8));
	}
	
	public static String decode64(String val) {
		return new String(BASE64_DECODER.decode(val), StandardCharsets.UTF_8);
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
	
}
