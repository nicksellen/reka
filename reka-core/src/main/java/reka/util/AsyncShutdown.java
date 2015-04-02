package reka.util;

import static java.util.Arrays.asList;
import static reka.util.Util.unchecked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface AsyncShutdown {
	
	void shutdown(Result res);
	default void shutdownAndWait() {
		FutureResult f = resultFuture();
		shutdown(f);
		try {
			f.future().get();
		} catch (InterruptedException | ExecutionException e) {
			throw unchecked(e);
		}
	}
	
	public static void shutdownAll(Collection<? extends AsyncShutdown> things, Result res) {
		shutdownAll(things).whenComplete((nothing, t) -> {
			if (t == null) {
				res.complete();
			} else {
				res.completeExceptionally(t);
			}
		});
	}
	
	public static CompletableFuture<Void> shutdownAll(AsyncShutdown... things) {
		return shutdownAll(asList(things));
	}
	
	public static CompletableFuture<Void> shutdownAll(Collection<? extends AsyncShutdown> things) {
		
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		
		things.forEach(thing -> {
			FutureResult res = AsyncShutdown.resultFuture();
			futures.add(res.future());
			try {
				thing.shutdown(new AsyncShutdown.Result() {
	
					@Override
					public void complete() {
						if (!res.future().isDone()) {
							res.future().complete(null);
						}
					}
					
					@Override
					public void completeExceptionally(Throwable t) {
						if (!res.future().isDone()) {
							res.future().completeExceptionally(t);
						}
					}
					
				});	
			} catch (Throwable t) {
				if (!res.future().isDone()) {
					res.future().completeExceptionally(t);
				}
			}
		});
		
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
	}
	
	public static interface Result {
		void complete();
		void completeExceptionally(Throwable t);
	}
	
	public static class FutureResult implements Result {

		private final CompletableFuture<Void> future = new CompletableFuture<>();
		
		@Override
		public void complete() {
			future.complete(null);
		}

		@Override
		public void completeExceptionally(Throwable t) {
			future.completeExceptionally(t);
		}
		
		public CompletableFuture<Void> future() {
			return future;
		}
		
	}
	
	public static FutureResult resultFuture() {
		return new FutureResult();
	}
	
}
