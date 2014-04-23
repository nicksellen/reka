package reka.api.run;

import reka.api.data.Data;
import reka.api.data.MutableData;

public interface EverythingSubscriber extends Subscriber {
	
	public static final EverythingSubscriber DO_NOTHING = new EverythingSubscriber() {

		@Override
		public void ok(MutableData data) { }

		@Override
		public void halted() { }

		@Override
		public void error(Data data, Throwable t) { }
		
	};
	
	static class Wrapper implements EverythingSubscriber {

		private final Subscriber subscriber;
		
		Wrapper(Subscriber subscriber) {
			this.subscriber = subscriber;
		}
		
		@Override
		public void ok(MutableData data) {
			subscriber.ok(data);
		}

		@Override
		public void halted() { }

		@Override
		public void error(Data data, Throwable t) { }
		
	}
	
	public static EverythingSubscriber wrap(Subscriber subcriber) {
		if (subcriber instanceof EverythingSubscriber) {
			return (EverythingSubscriber) subcriber;
		} else {
			return new Wrapper(subcriber);
		}
	}
	
	public void halted();
	public void error(Data data, Throwable t);
}