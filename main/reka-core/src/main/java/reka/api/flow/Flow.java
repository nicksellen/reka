package reka.api.flow;

import java.util.concurrent.ExecutorService;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.EverythingSubscriber;
import reka.api.run.Subscriber;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public interface Flow {
	
    long id();
    Path name();
    String fullName();
    
    void run();
    void run(EverythingSubscriber run);
    void run(ListeningExecutorService executor, MutableData data, EverythingSubscriber subscriber);

    FlowRun prepare();

    default void run(Subscriber run) {
    	run(EverythingSubscriber.wrap(run));
    }

    default void run(ExecutorService executor, MutableData data, EverythingSubscriber subscriber) {
    	run(MoreExecutors.listeningDecorator(executor), data, subscriber);
    }
}
