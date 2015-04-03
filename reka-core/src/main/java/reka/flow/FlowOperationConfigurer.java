package reka.flow;

public interface FlowOperationConfigurer <T extends FlowOperation> {
	public T build();
}
