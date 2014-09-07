package reka.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowRun;
import reka.api.run.EverythingSubscriber;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DefaultFlow implements Flow {
	
	private final static AtomicLong flowIds = new AtomicLong();
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("flow");
	
	private static final ListeningExecutorService DEFAULT_EXECUTOR = 
		MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
	
	private final long id;
	private final Path name;
	private final String fullName;
	private final Node head;
	
	public DefaultFlow(Path name, Node head) {
		this.id = flowIds.incrementAndGet();
		this.name = name;
	    this.head = head;
	    this.fullName = name.slashes();
	}
	
	public class DefaultFlowRun implements FlowRun {
		
		private EverythingSubscriber subscriber;
		private MutableData data;
		private ExecutorService executor;
		
		@Override
        public FlowRun complete(EverythingSubscriber subscriber) {
			this.subscriber = subscriber;
			return this;
		}
		
		@Override
        public FlowRun executor(ExecutorService executor) {
			this.executor = executor;
			return this;
		}
		
		@Override
        public FlowRun data(MutableData value) {
			data = value;
			return this;
		}
		
		@Override
        public void run() {
			if (data == null) data = MutableMemoryData.create();
			if (executor == null) executor = DEFAULT_EXECUTOR;
			if (subscriber == null) subscriber = EverythingSubscriber.wrap((data) -> {});
			DefaultFlow.this.run(executor, data, subscriber);
		}
	}
	
	@Override
    public FlowRun prepare() {
		return new DefaultFlowRun();
	}
	
	@Override
    public void run() {
		run((data) -> {});
	}
	
	@Override
	public void run(EverythingSubscriber subscriber) {
		run(DEFAULT_EXECUTOR, MutableMemoryData.create(), subscriber);
	}
	
	@Override
	public void run(ExecutorService executor, MutableData data, EverythingSubscriber subscriber) {
		head.call(data, new DefaultFlowContext(id, executor, subscriber));
	}
	
	@Override
	public long id() {
		return id;
	}
	
	@Override
    public Path name() {
		return name;
	}
	
	@Override
	public String fullName() {
		return fullName;
	}
	
}