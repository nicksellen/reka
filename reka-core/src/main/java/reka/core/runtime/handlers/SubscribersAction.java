package reka.core.runtime.handlers;

import java.util.Arrays;
import java.util.Collection;

import reka.api.data.MutableData;
import reka.api.run.Subscriber;
import reka.core.runtime.FlowContext;

public class SubscribersAction implements ActionHandler {
	
	private final Collection<Subscriber> subscribers;
	
	public SubscribersAction(Subscriber... subscribers) {
		this(Arrays.asList(subscribers));
	}
	
	public SubscribersAction(Collection<Subscriber> subscribers) {
		this.subscribers = subscribers;
	}

	@Override
	public void call(MutableData data, FlowContext context) {
		for (Subscriber subscriber : subscribers) {
			try {
				subscriber.ok(data);
			} catch (Throwable t) {
				//t.printStackTrace();
				// continue...
			}
		}
	}

}
