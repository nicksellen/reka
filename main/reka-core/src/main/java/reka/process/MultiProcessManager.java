package reka.process;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class MultiProcessManager implements ProcessManager {

	private final BlockingDeque<Entry<String,Consumer<String>>> q = new LinkedBlockingDeque<>();
	private final ProcessBuilder builder;
	private final Collection<SingleProcessManager> all = new ArrayList<>();
	
	public MultiProcessManager(ProcessBuilder builder, int count, boolean noreply) {
		this.builder = builder;
		for (int i = 0; i < count; i++) {
			all.add(new SingleProcessManager(this.builder, q, noreply));
		}
	}
	
	@Override
	public void run(String input, Consumer<String> consumer) {
		q.offer(createEntry(input, consumer));
	}


	@Override
	public void run(String input) {
		run(input, null);
	}

	@Override
	public void kill() {
		all.forEach(m -> m.kill());
	}

	@Override
	public void addLineTrigger(Consumer<String> consumer) {
		all.forEach(m -> m.addLineTrigger(consumer));
	}
	
}