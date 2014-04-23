package reka.api.run;

import java.util.function.Function;

import reka.api.data.Data;

public interface AsyncOperationSupplier<T extends AsyncOperation> extends OperationSupplier<T>, Function<Data,T> {
}
