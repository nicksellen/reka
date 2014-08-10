package reka.process;

import java.util.function.Consumer;

public interface ProcessManager {

	public abstract void run(String input, Consumer<String> consumer);

	public abstract void kill();

}