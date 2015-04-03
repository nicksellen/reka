package reka.process;

import static reka.api.Path.path;

import java.util.function.Consumer;

import reka.api.Path;
import reka.module.setup.StatusDataProvider;

public interface ProcessManager extends StatusDataProvider {
	
	static final Path Q_PATH = path("q");
	
	void start();
	void send(String input);
	void send(String input, Consumer<String> rely);
	void shutdown();
	void addListener(Consumer<String> reply);
	
}