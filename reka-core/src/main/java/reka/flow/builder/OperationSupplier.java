package reka.flow.builder;

import java.util.function.Supplier;

import reka.flow.FlowOperation;

public interface OperationSupplier <T extends FlowOperation> extends Supplier<T> {
	boolean isRouter();
}
