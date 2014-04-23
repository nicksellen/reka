package reka.api.run;

import reka.api.data.Data;
import reka.api.flow.FlowOperation;

import com.google.common.base.Optional;

public interface OperationSupplier<T extends FlowOperation> {
	
	public static Optional<FlowOperation> supply(OperationSupplier<? extends FlowOperation> supplier, Data initializationData) {
		if (supplier instanceof SyncOperationSupplier) {
			return Optional.of(((SyncOperationSupplier<? extends FlowOperation>) supplier).apply(initializationData));
		} else if (supplier instanceof AsyncOperationSupplier) {
			return Optional.of(((AsyncOperationSupplier<? extends FlowOperation>) supplier).apply(initializationData));
		} else if (supplier instanceof RouterOperationSupplier) {
			return Optional.of(((RouterOperationSupplier<? extends FlowOperation>) supplier).apply(initializationData));
		} else {
			return Optional.absent();
		}
	}
	
}
