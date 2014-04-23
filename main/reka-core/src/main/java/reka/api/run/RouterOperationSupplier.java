package reka.api.run;

import java.util.function.Function;

import reka.api.data.Data;

public interface RouterOperationSupplier<T extends RoutingOperation> extends OperationSupplier<T>, Function<Data,T> {
}
