package reka.flow.ops;

import reka.data.Data;
import reka.data.MutableData;

public interface Subscriber {
	
	void ok(MutableData data);
	
	default void halted() {
		System.err.printf("ignored subscriber halt\n");
	}
	
	default void error(Data data, Throwable t) {
		System.err.printf("ignored subscriber error\n");
		t.printStackTrace();
	}
	
	public static final Subscriber DO_NOTHING = new Subscriber() {
		@Override
		public void ok(MutableData data) { }
	};
	
}
