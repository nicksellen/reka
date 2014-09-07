package reka.process;

import java.util.function.Consumer;

public interface ProcessManager {
	void run(String input);
	void run(String input, Consumer<String> consumer);
	void kill();
	void addLineTrigger(Consumer<String> consumer);
}