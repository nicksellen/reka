package reka.util;

@FunctionalInterface
public interface ThrowingConsumer<T> {
	void accept(T val) throws Exception;
}
