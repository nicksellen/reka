package reka.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.LongAdder;

import reka.data.MutableData;
import reka.flow.ops.Subscriber;
import reka.identity.IdentityStoreReader;
import reka.util.Path;

public interface Flow extends Comparable<Flow> {
	
    long id();
    Path name();
    String fullName();
    
    void run();
    void run(Subscriber run);
    
    FlowStats stats();

    FlowRun prepare();
    
    void run(ExecutorService operationExecutor, ExecutorService coordinationExecutor, MutableData data, Subscriber subscriber, IdentityStoreReader store, boolean statsEnabled);

    default void runWithSingleThreadedExecutor(ExecutorService singleThreadedCoordinator, MutableData data, Subscriber subscriber, IdentityStoreReader store, boolean statsEnabled) {
    	run(singleThreadedCoordinator, singleThreadedCoordinator, data, subscriber, store, statsEnabled);
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

	public static class FlowStats {
		public final LongAdder requests = new LongAdder();
		public final LongAdder completed = new LongAdder();
		public final LongAdder errors = new LongAdder();
		public final LongAdder halts = new LongAdder();
	}
    
}
