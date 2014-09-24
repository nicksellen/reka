package reka.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowRun;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;
import reka.util.Util;

public class DefaultFlow implements Flow {

	private final FlowStats stats = new FlowStats();
	private final static AtomicLong ids = new AtomicLong();
	
	private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
	
	private final long id;
	private final Path name;
	private final String fullName;
	private final Node head;
	
	public DefaultFlow(Path name, Node head) {
		this.id = ids.incrementAndGet();
		this.name = name;
	    this.head = head;
	    this.fullName = name.slashes();
	}
	
	public class DefaultFlowRun implements FlowRun {
		
		private Subscriber subscriber;
		private MutableData data;
		private ExecutorService executor;
		private boolean stats = true;
		
		@Override
        public FlowRun complete(Subscriber subscriber) {
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
		public FlowRun stats(boolean enabled) {
			stats = enabled;
			return this;
		}
		
		@Override
        public void run() {
			if (data == null) data = MutableMemoryData.create();
			if (executor == null) executor = DEFAULT_EXECUTOR;
			if (subscriber == null) subscriber = Subscriber.DO_NOTHING;
			DefaultFlow.this.run(executor, data, subscriber, stats);
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
	public void run(Subscriber subscriber) {
		run(DEFAULT_EXECUTOR, MutableMemoryData.create(), subscriber, true);
	}
	
	@Override
	public void run(ExecutorService executor, MutableData data, Subscriber subscriber, boolean statsEnabled) {
		DefaultFlowContext.create(id, executor, subscriber, statsEnabled ? stats : null).call(head, (d, c, t) -> {
			subscriber.error(d, t);
		}, data);
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

	@Override
	public int compareTo(Flow o) {
		return 0;
	}

	@Override
	public FlowStats stats() {
		return stats;
	}
	
}