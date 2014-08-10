package reka.util;

import static java.lang.String.format;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
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
		
	public static interface ThrowingRunnable {
		void run() throws Throwable;
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
}
