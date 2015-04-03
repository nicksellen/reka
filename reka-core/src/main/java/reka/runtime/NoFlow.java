package reka.runtime;

import static reka.api.Path.empty;

import java.util.concurrent.ExecutorService;

import reka.api.IdentityStoreReader;
import reka.api.Path;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.Flow;
import reka.flow.FlowRun;
import reka.flow.ops.Subscriber;

public class NoFlow implements Flow {

	public static final Flow INSTANCE = new NoFlow();

	private NoFlow() {
	}

	private final FlowStats stats = new FlowStats();

	@Override
	public long id() {
		return 0;
	}

	@Override
	public FlowRun prepare() {
		return new NoFlowRun();
	}

	@Override
	public void run() {
		new NoFlowRun().run();
	}

	@Override
	public void run(Subscriber run) {
		new NoFlowRun().complete(run).run();
	}

	@Override
	public Path name() {
		return empty();
	}

	@Override
	public String fullName() {
		return "";
	}

	private class NoFlowRun implements FlowRun {

		private MutableData data = MutableMemoryData.create();
		private Subscriber subscriber;

		@Override
		public FlowRun complete(Subscriber subscriber) {
			this.subscriber = subscriber;
			return this;
		}

		@Override
		public FlowRun operationExecutor(ExecutorService executor) {
			return this;
		}

		@Override
		public FlowRun coordinationExecutor(ExecutorService executor) {
			return this;
		}

		@Override
		public FlowRun mutableData(MutableData value) {
			data = value;
			return this;
		}

		@Override
		public FlowRun stats(boolean enabled) {
			return this;
		}

		@Override
		public FlowRun store(IdentityStoreReader value) {
			return this;
		}

		@Override
		public void run() {
			subscriber.ok(data);
		}

	}

	@Override
	public int compareTo(Flow o) {
		return 0;
	}

	@Override
	public FlowStats stats() {
		return stats;
	}

	@Override
	public void run(ExecutorService coordinationExecutor,
			ExecutorService operationExecutor, MutableData data,
			Subscriber subscriber, IdentityStoreReader store,
			boolean statsEnabled) {
		new NoFlowRun().mutableData(data).complete(subscriber)
				.stats(statsEnabled).run();
	}

}
