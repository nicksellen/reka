package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Subscriber;

public interface Flow extends Comparable<Flow> {
	
    long id();
    Path name();
    String fullName();
    
    void run();
    void run(Subscriber run);

    FlowRun prepare();
    
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
