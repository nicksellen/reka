package reka.api.flow;

import static com.google.common.base.Preconditions.checkNotNull;
import reka.api.run.OperationSupplier;

public class FlowOperationSuppliers {
	
	@SuppressWarnings("unchecked")
	public static Class<? extends FlowOperation> operationClassOf(OperationSupplier<?> supplier) {
		checkNotNull(supplier, "you must pass in a supplier!");
		try {
			return (Class<? extends FlowOperation>) supplier.getClass().getMethod("supply").getReturnType();
			
			/*
			if (supplier instanceof SingletonSupplier) {
				// return the *actual* class of the operation, not the generic method declaration
				return ((SingletonSupplier<? extends FlowOperation>) supplier).operation.getClass();
			} else {
				return (Class<? extends FlowOperation>) supplier.getClass().getMethod("supply").getReturnType();
			}
			*/
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*

	public static <T extends FlowOperation> OperationSupplier<T> singletonSupplier(T operation) {
		return new SingletonSupplier<T>(operation);
	}
	
	public static OperationSupplier<SyncOperation> singletonSyncSupplier(SyncOperation operation) {
		return singletonSupplier(operation);
	}
	
	public static OperationSupplier<AsyncOperation> singletonAsyncSupplier(AsyncOperation operation) {
		return singletonSupplier(operation);
	}

	private static class SingletonSupplier<T extends FlowOperation> implements OperationSupplier<T> {

		private final T operation;
		
		SingletonSupplier(T operation) {
			this.operation = operation;
		}
		
		@Override
		public T supply() {
			return operation;
		}
		
	}
	
	*/
	
}
