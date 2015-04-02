package reka.api.flow;


public interface FlowOperationConfigurer <T extends FlowOperation> {
	
	public T build();
	
}
