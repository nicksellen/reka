package reka.api.data.versioned;

import reka.api.data.Data;
import reka.api.data.DataMutation;

import com.google.common.util.concurrent.ListenableFuture;

public interface VersionedAtomicMutableData extends VersionedData {
	
	Data snapshot();

	VersionedAtomicMutableData.Mutation createMutation();

	public interface Mutation extends DataMutation<Mutation> {
		ListenableFuture<? extends DataVersion> commit();
	}

}
