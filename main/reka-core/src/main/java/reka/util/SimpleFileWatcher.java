package reka.util;

import static reka.util.Util.unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleFileWatcher {
	
	private static final WatchService watcher;

	static {
		try {
			watcher = FileSystems.getDefault().newWatchService();

			new Thread() {
				@Override
				public void run() {
					WatchKey watch;
					
					while (true) {
						try {
							watch = watcher.take();
						} catch (InterruptedException e) {
							throw unchecked(e);
						}
						List<WatchEvent<?>> events = watch.pollEvents();
						
						watch.reset();
						
						for (WatchEvent<?> event : events) {
							
							@SuppressWarnings("unchecked")
							Kind<Path> kind = (Kind<Path>) event.kind();
							java.nio.file.Path context = (java.nio.file.Path) event.context();
							if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
								java.nio.file.Path dir = (java.nio.file.Path) watch.watchable();
								String filename = dir.resolve(context).toAbsolutePath().toString();
								if (watchers.containsKey(filename)) {
									watchers.get(filename).run();
								}
							}
						}
					}
				}
			}.start();

		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	private final static Map<String, Runnable> watchers = new ConcurrentHashMap<>();
	
	public void watch(String filename, Runnable runnable) {
		if (!new File(filename).exists()) throw new IllegalArgumentException(String.format("%s doesn't exist", filename));
		watchers.put(filename, runnable);
		java.nio.file.Path directory = new File(filename).toPath().getParent();
		try {
			directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			throw unchecked(e);
		}	
	}
}
