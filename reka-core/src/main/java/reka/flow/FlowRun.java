
package reka.flow;

import java.util.concurrent.ExecutorService;

import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Subscriber;
import reka.identity.IdentityStoreReader;

public interface FlowRun {
    FlowRun complete(Subscriber subscriber);
    FlowRun operationExecutor(ExecutorService executor);
    FlowRun coordinationExecutor(ExecutorService executor);
    FlowRun mutableData(MutableData value);
    FlowRun store(IdentityStoreReader value);
    FlowRun stats(boolean enabled);
    
    void run();
    
    default void run(Subscriber subscriber) {
    	complete(subscriber);
    	run();
    }
    
    default FlowRun data(Data data) {
    	MutableData mutableData = MutableMemoryData.create();
    	mutableData.merge(data);
    	return mutableData(mutableData);
    }
    
}
