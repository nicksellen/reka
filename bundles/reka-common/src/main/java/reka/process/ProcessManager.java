package reka.process;

import java.util.function.Consumer;

import reka.core.setup.StatusDataProvider;

public interface ProcessManager extends StatusDataProvider {
	void send(String input);
	void send(String input, Consumer<String> rely);
	void kill();
	void addListener(Consumer<String> reply);
}