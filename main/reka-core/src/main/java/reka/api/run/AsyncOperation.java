package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncOperation extends FlowOperation {
	public ListenableFuture<MutableData> call(MutableData data);
}