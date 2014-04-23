package reka.api.flow;

public interface FlowOperation {
	public interface Cancellable {
		public void cancelled();
	}
}