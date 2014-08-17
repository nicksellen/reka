package reka.api.run;

import java.util.function.Function;

import reka.api.data.Data;

public interface DataOperationSupplier<T extends DataOperation> extends OperationSupplier<T>, Function<Data,T> {
}
