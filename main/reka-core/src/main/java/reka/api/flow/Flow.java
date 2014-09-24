package reka.api.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Subscriber;

public interface Flow extends Comparable<Flow> {
	
    long id();
    Path name();
    String fullName();
    
    void run();
    void run(Subscriber run);
    
    FlowStats stats();

    FlowRun prepare();
    
    void run(ExecutorService executor, MutableData data, Subscriber subscriber, boolean statsEnabled);
    
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

	public static class FlowStats {
		public final LongAdder requests = new LongAdder();
		public final LongAdder completed = new LongAdder();
		public final LongAdder errors = new LongAdder();
		public final LongAdder halts = new LongAdder();
	}
    
}
