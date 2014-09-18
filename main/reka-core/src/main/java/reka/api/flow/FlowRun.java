
package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.data.MutableData;
import reka.api.run.Subscriber;

public interface FlowRun {
    FlowRun complete(Subscriber subscriber);
    FlowRun executor(ExecutorService executor);
    FlowRun data(MutableData value);
    void run();
}
