package reka.process;

import static reka.util.Path.path;

import java.util.function.Consumer;

import reka.module.setup.StatusDataProvider;
import reka.util.Path;

public interface ProcessManager extends StatusDataProvider {
	
	static final Path Q_PATH = path("q");
	
	void start();
	void send(String input);
	void send(String input, Consumer<String> rely);
	void shutdown();
	void addListener(Consumer<String> reply);
	
}