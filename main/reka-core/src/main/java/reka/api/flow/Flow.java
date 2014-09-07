package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;

public interface Flow extends Comparable<Flow> {
	
    long id();
    Path name();
    String fullName();
    
    void run();
    void run(EverythingSubscriber run);

    FlowRun prepare();

    default void run(Subscriber run) {
    	run(EverythingSubscriber.wrap(run));
    }

    default void run(ExecutorService executor, MutableData data, EverythingSubscriber subscriber) {
    	run(executor, data, subscriber);
    }
    
    default void run(ExecutorService executor, MutableData data, Subscriber subscriber) {
    	run(executor, data, subscriber);
    }
    
    @Override
    default int compareTo(Flow o) {
    	if (id() != o.id()) {
    		return Long.compare(id(), o.id());
    	} else if (!name().equals(o.name())) {
    		return name().compareTo(o.name());
    	} else {
    		return 0;
    	}
    }
}
