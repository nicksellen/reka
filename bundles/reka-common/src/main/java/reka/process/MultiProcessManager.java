package reka.process;

import static reka.util.Util.createEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;
import reka.core.setup.StatusDataProvider;

public class MultiProcessManager implements ProcessManager {

	private final BlockingDeque<Entry<String,Consumer<String>>> q = new LinkedBlockingDeque<>();
	private final ProcessBuilder builder;
	private final Collection<SingleProcessManager> all = new ArrayList<>();
		
	public MultiProcessManager(ProcessBuilder builder, int count, boolean noreply) {
		this.builder = builder;
		for (int i = 0; i < count; i++) {
			all.add(new SingleProcessManager(this.builder, noreply, q));
		}
	}
	
	@Override
	public void send(String input, Consumer<String> consumer) {
		q.offer(createEntry(input, consumer));
	}


	@Override
	public void send(String input) {
		send(input, null);
	}

	@Override
	public void kill() {
		all.forEach(m -> m.kill());
	}

	@Override
	public void addListener(Consumer<String> consumer) {
		all.forEach(m -> m.addListener(consumer));
	}

	@Override
	public boolean up() {
		return all.stream().anyMatch(StatusDataProvider::up);
	}
	
	@Override
	public void statusData(MutableData data) {
		data.putList("processes", list -> {
			all.forEach(item -> {
				list.addMap(map -> {
					MutableData itemData = MutableMemoryData.create();
					item.statusData(itemData);
					itemData.forEachContent((path, content) -> {
						map.put(path, content);
					});
					map.putBool("up", item.up());
				});
			});
		});
	}
	
}