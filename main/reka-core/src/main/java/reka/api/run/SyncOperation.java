package reka.api.run;

import com.google.common.util.concurrent.ListeningExecutorService;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;
import reka.core.builder.SyncToAsync;

public interface SyncOperation extends FlowOperation {
	public MutableData call(MutableData data);
	default public AsyncOperation toAsync(ListeningExecutorService executor) {
		return new SyncToAsync(this, executor);
	}
}