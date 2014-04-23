package reka.api.run;

import java.util.function.Function;

import reka.api.data.Data;

public interface SyncOperationSupplier<T extends SyncOperation> extends OperationSupplier<T>, Function<Data,T> {
}
