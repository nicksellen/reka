package reka.core.builder;

import java.util.concurrent.Callable;

import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.SyncOperation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class SyncToAsync implements AsyncOperation {

	private final SyncOperation operation;
	private final ListeningExecutorService executor;
	
	public SyncToAsync(SyncOperation operation, ListeningExecutorService executor) {
		this.operation = operation;
		this.executor = executor;
	}

	@Override
	public ListenableFuture<MutableData> call(MutableData data) {
		return executor.submit(new Callable<MutableData>(){

			@Override
			public MutableData call() throws Exception {
				return operation.call(data);
			}
			
		});
	}

}
