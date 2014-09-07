
package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.data.MutableData;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;

public interface FlowRun {
    default FlowRun complete(Subscriber subscriber) {
    	return complete(EverythingSubscriber.wrap(subscriber));
    }
    FlowRun complete(EverythingSubscriber subscriber);
    FlowRun executor(ExecutorService executor);
    FlowRun data(MutableData value);
    void run();
}
