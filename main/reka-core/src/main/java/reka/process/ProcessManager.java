package reka.process;

import java.util.function.Consumer;

public interface ProcessManager {
	void send(String input);
	void send(String input, Consumer<String> rely);
	void kill();
	void addListener(Consumer<String> reply);
}