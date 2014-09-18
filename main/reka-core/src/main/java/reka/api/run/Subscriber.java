package reka.api.run;

import reka.api.data.Data;
import reka.api.data.MutableData;

public interface Subscriber {
	
	void ok(MutableData data);
	default void halted() {}
	default void error(Data data, Throwable t) {}
	
	public static final Subscriber DO_NOTHING = new Subscriber() {
		@Override
		public void ok(MutableData data) { }
	};
	
}
