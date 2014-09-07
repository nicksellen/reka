package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;
import reka.core.builder.SyncToAsync;

import com.google.common.util.concurrent.ListeningExecutorService;

public interface SyncOperation extends SimpleFlowOperation {
	public MutableData call(MutableData data);
	default public AsyncOperation toAsync(ListeningExecutorService executor) {
		return new SyncToAsync(this, executor);
	}
}