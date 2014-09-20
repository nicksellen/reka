package reka.api.run;

import reka.api.data.Data;
import reka.api.data.MutableData;

public interface Subscriber {
	
	void ok(MutableData data);
	
	default void halted() {
		System.err.printf("halted, but ignore :(\n");
	}
	
	default void error(Data data, Throwable t) {
		System.err.printf("errored, but ignore :( - %s\n", t.getMessage());
	}
	
	public static final Subscriber DO_NOTHING = new Subscriber() {
		@Override
		public void ok(MutableData data) { }
	};
	
}
