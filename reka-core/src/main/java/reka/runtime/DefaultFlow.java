package reka.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import reka.api.IdentityStore;
import reka.api.IdentityStoreReader;
import reka.api.Path;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.FlowRun;
import reka.flow.ops.Subscriber;

public class DefaultFlow implements Flow {

	private final FlowStats stats = new FlowStats();
	private final static AtomicLong ids = new AtomicLong();

	private static final ExecutorService DEFAULT_OPERATION_EXECUTOR = Executors.newCachedThreadPool();
	private static final ExecutorService DEFAULT_COORDINATOR_EXECUTOR = Executors.newSingleThreadExecutor();
	
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
		private ExecutorService operationExecutor;
		private ExecutorService coordinationExecutor;
		private IdentityStoreReader store;
		private boolean stats = true;
		
		@Override
        public FlowRun complete(Subscriber subscriber) {
			this.subscriber = subscriber;
			return this;
		}
		
		@Override
        public FlowRun operationExecutor(ExecutorService executor) {
			this.operationExecutor = executor;
			return this;
		}
		
		@Override
        public FlowRun coordinationExecutor(ExecutorService executor) {
			this.coordinationExecutor = executor;
			return this;
		}
		
		@Override
        public FlowRun mutableData(MutableData value) {
			data = value;
			return this;
		}

		@Override
		public FlowRun store(IdentityStoreReader value) {
			store = value;
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
			if (operationExecutor == null) operationExecutor = DEFAULT_OPERATION_EXECUTOR;
			if (coordinationExecutor == null) coordinationExecutor = DEFAULT_COORDINATOR_EXECUTOR;
			if (subscriber == null) subscriber = Subscriber.DO_NOTHING;
			if (store == null) store = IdentityStore.emptyReader();
			DefaultFlow.this.run(operationExecutor, coordinationExecutor, data, subscriber, store, stats);
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
		run(DEFAULT_OPERATION_EXECUTOR, DEFAULT_COORDINATOR_EXECUTOR, MutableMemoryData.create(), subscriber, IdentityStore.emptyReader(), true);
	}
	
	@Override
	public void run(ExecutorService operationExecutor, ExecutorService coordinationExecutor, MutableData data, Subscriber subscriber, IdentityStoreReader store, boolean statsEnabled) {
		DefaultFlowContext.create(id, operationExecutor, coordinationExecutor, subscriber, store, statsEnabled ? stats : null).handleAction(head, (d, c, t) -> {
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