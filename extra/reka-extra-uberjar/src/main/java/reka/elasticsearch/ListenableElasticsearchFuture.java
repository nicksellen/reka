package reka.elasticsearch;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.elasticsearch.action.ListenableActionFuture;

import com.google.common.util.concurrent.ListenableFuture;

public class ListenableElasticsearchFuture <T> implements ListenableFuture<T> {
	
	private final ListenableActionFuture<T> future;
	
	public static <T> ListenableElasticsearchFuture<T> wrap(ListenableActionFuture<T> input) {
		return new ListenableElasticsearchFuture<>(input);
	}
	
	ListenableElasticsearchFuture(ListenableActionFuture<T> input) {
		this.future = input;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}

	@Override
	public void addListener(final Runnable listener, final Executor executor) {
		if (executor != null) {
			// the es future doesn't let me specify the executor...
			future.addListener(new Runnable(){

				@Override
				public void run() {
					executor.execute(listener);
				}
				
			});
		} else {
			future.addListener(listener);
		}
	}
	
}