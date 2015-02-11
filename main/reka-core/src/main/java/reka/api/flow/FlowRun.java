
package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;

public interface FlowRun {
    FlowRun complete(Subscriber subscriber);
    FlowRun operationExecutor(ExecutorService executor);
    FlowRun coordinationExecutor(ExecutorService executor);
    FlowRun mutableData(MutableData value);
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
