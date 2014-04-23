package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;

public interface SyncOperation extends FlowOperation {
	public MutableData call(MutableData data);
}