package reka.core.runtime;

import static reka.api.Path.empty;

import java.util.concurrent.ExecutorService;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.flow.Flow;
import reka.api.flow.FlowRun;
import reka.api.run.Subscriber;
import reka.core.data.memory.MutableMemoryData;

public class NoFlow implements Flow {

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
		public FlowRun executor(ExecutorService executor) {
			return this;
		}

		@Override
		public FlowRun data(MutableData value) {
			data = value;
			return this;
		}

		@Override
		public FlowRun stats(boolean enabled) {
			return this;
		}

		@Override
		public void run() {
			subscriber.ok(data);
		}
		
	}

	@Override
	public void run(ExecutorService executor, MutableData data, Subscriber subscriber, boolean statsEnabled) {
		new NoFlowRun().executor(executor).data(data).complete(subscriber).stats(statsEnabled).run();
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
