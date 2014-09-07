package reka.core.builder;

import java.util.function.Supplier;

import reka.api.flow.FlowOperation;

public interface OperationSupplier <T extends FlowOperation> extends Supplier<T> {
	boolean isRouter();
}
