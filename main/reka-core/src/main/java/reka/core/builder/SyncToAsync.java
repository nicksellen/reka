package reka.core.builder;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class SyncToAsync implements AsyncOperation {

	private final Operation operation;
	private final ListeningExecutorService executor;
	
	public SyncToAsync(Operation operation, ListeningExecutorService executor) {
		this.operation = operation;
		this.executor = executor;
	}

	@Override
	public ListenableFuture<MutableData> call(MutableData data) {
		return executor.submit(() -> operation.call(data));
	}

}
