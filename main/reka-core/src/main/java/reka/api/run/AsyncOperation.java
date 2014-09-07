package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncOperation extends SimpleFlowOperation {
	public ListenableFuture<MutableData> call(MutableData data);
}