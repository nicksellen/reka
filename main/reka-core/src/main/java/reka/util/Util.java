package reka.util;

import static java.lang.String.format;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
	
	public static Integer[] removedValues(int[] from, int[] to) {
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
		return removed.toArray(new Integer[removed.size()]);
	}
	public static <K,V> Entry<K, V> createEntry(K key, V value) {
		return new AbstractMap.SimpleEntry<K,V>(key, value);
	}
}
