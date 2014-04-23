package reka.api.data;

import com.google.common.util.concurrent.ListenableFuture;

public interface AtomicMutableData extends Data {

	AtomicMutableData atomicMutableCopy();

	DataMutation<AtomicMutableData.Mutation> createMutation();

	public interface Mutation extends DataMutation<Mutation> {
		ListenableFuture<Void> commit();
	}

}
