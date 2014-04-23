package reka.core.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reka.api.flow.FlowConnection;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;
import reka.api.run.AsyncOperation;
import reka.api.run.AsyncOperationSupplier;
import reka.api.run.RouterOperationSupplier;
import reka.api.run.RoutingOperation;
import reka.api.run.SyncOperation;
import reka.api.run.SyncOperationSupplier;
import reka.core.config.NoOpSupplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class FlowSegments extends AbstractFlowNode {
	
	public static interface SegmentCollector {
		void addLabel(String label);
		void add(FlowSegment seg);
		void add(String name, FlowSegment seg);
		void add(String name, Collection<FlowSegment> segs);
		void add(String name, Consumer<SegmentCollector> named);
		void sequential(Consumer<SegmentCollector> seq);
		void parallel(Consumer<SegmentCollector> par);
		void routerNode(String name, RouterOperationSupplier<?> supplier);
		void node(String name, SyncOperationSupplier<?> supplier);
		void asyncNode(String name, AsyncOperationSupplier<?> supplier);
	}
	
	private static class DefaultSegmentCollector implements SegmentCollector {
		
		private final Collection<FlowSegment> segments = new ArrayList<>();
		private String label;
		
		@Override
		public void addLabel(String label) {
			this.label = label;
		}
		
		@Override
		public void add(FlowSegment segment) {
			this.segments.add(segment);
		}
		
		@Override
		public void add(String name, FlowSegment segment) {
			add(namedInput(name, segment));
		}

		@Override
		public void add(String name, Collection<FlowSegment> segment) {
			add(namedInput(name, FlowSegments.seq(segment)));
		}
				
		@Override
		public void add(String name, Consumer<SegmentCollector> coll) {
			add(namedInput(name, new DefaultSegmentCollector().collectSequential(coll)));
		}
		
		@Override
		public void sequential(Consumer<SegmentCollector> seq) {
			add(new DefaultSegmentCollector().collectSequential(seq));
		}
		
		@Override
		public void parallel(Consumer<SegmentCollector> par) {
			add(new DefaultSegmentCollector().collectParallel(par));
		}
		
		public void routerNode(String name, RouterOperationSupplier<?> supplier) {
			add(new OperationFlowNode(name, supplier));
		}
		
		public void node(String name, SyncOperationSupplier<?> supplier) {
			add(new OperationFlowNode(name, supplier));
		}
		
		public void asyncNode(String name, AsyncOperationSupplier<?> supplier) {
			add(new OperationFlowNode(name, supplier));
		}
		
		private FlowSegment collectSequential(Consumer<SegmentCollector> consumer) {
			consumer.accept(this);
			FlowSegment main = seq(segments);
			return label != null ? label(label, main) : main;
		}

		private FlowSegment collectParallel(Consumer<SegmentCollector> consumer) {
			consumer.accept(this);
			FlowSegment main = par(segments);
			return label != null ? label(label, main) : main;
		}
		
	}
	
	public static FlowSegment sequential(Consumer<SegmentCollector> seq) {
		return new DefaultSegmentCollector().collectSequential(seq);
	}

	public static FlowSegment parallel(Consumer<SegmentCollector> par) {
		return new DefaultSegmentCollector().collectParallel(par);
	}
	
	public static FlowSegment seq(Collection<? extends FlowSegment> segments) {
		return Sequential.of(segments);
	}

	public static FlowSegment seq(FlowSegment first, FlowSegment... rest) {
		return Sequential.of(first, rest);
	}
	
	/*
	public static FlowSegment tap(FlowSegment main, FlowSegment tap) {
		return new TapFlowSegment(main, tap);
	}
	
	public static FlowSegment passthrough(FlowSegment head, FlowSegment branch) {
		return new AppendWithBypass(head, branch, new String[]{});
	}
	
	public static FlowSegment afterWithBypass(FlowSegment head, FlowSegment branch, String... names) {
		return new AppendWithBypass(head, branch, names);
	}

	public static FlowSegment beforeWithBypass(FlowSegment head, FlowSegment branch, String branchName, String bypassName) {
		return new PrependWithBypass(head, branch, branchName, bypassName);
	}
	
	public static FlowSegment halt(FlowSegment... items) {
		FlowSegment[] ary = new FlowSegment[items.length + 1];
		System.arraycopy(items, 0, ary, 0, items.length);
		ary[ary.length - 1] = halt();
		return Sequential.of(ary);
	}
	
	public static FlowSegment halt() {
		return new Halt();
	}
	*/
	
	public static FlowSegment halt() {
		return new Halt();
	}
	
	public static FlowSegment par(Collection<? extends FlowSegment> items) {
	    checkArgument(!items.isEmpty(), "a parallel segment with no items doesn't make sense");
		return par(items.toArray(new FlowSegment[items.size()]));
	}
	
	public static FlowSegment par(FlowSegment... items) {
		return new Parallel(items);
	}
	
	public static FlowSegment select(FlowSegment... items) {
		return par(items);
	}
	
	public static FlowSegment embeddedFlow(String embeddedFlowName) {
		return new EmbeddedFlowNode(embeddedFlowName, embeddedFlowName);
	}

	public static FlowNode noop(String name) {
		return new OperationFlowNode(name, new NoOpSupplier());
	}
	
	public static FlowNode router(String name, RouterOperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier);
	}

	public static FlowNode sync(String name, Supplier<SyncOperation> supplier) {
		return sync(name, data -> supplier.get());
	}
	
	public static FlowNode async(String name, Supplier<AsyncOperation> supplier) {
		return async(name, data -> supplier.get());
	}
	
	public static FlowNode router(String name, Supplier<RoutingOperation> supplier) {
		return router(name, data -> supplier.get());
	}
	
	public static FlowNode sync(String name, SyncOperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier);
	}
	
	public static FlowNode background(String name, Supplier<SyncOperation> supplier) {
		return background(name, data -> supplier.get());
	}
	
	public static FlowNode background(String name, SyncOperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier).shouldUseAnotherThread(true);
	}
	
	public static FlowNode async(String name, AsyncOperationSupplier<?> supplier) {
		return new OperationFlowNode(name, supplier);
	}

	public static FlowNode startNode(String name) {
		return new OperationFlowNode(name, new NoOpSupplier()).isStart(true);
	}
	
	public static FlowNode subscribeableEndNode(String name) {
		return new SubscribeableEndFlowNode(name);
	}
	
	public static FlowSegment label(String label, FlowSegment... segment) {
		return new Labelled(label, Sequential.of(segment));
	}
	
	public static FlowSegment namedInput(String name, FlowSegment... segment) {
		return new NamedInput(name, Sequential.of(segment));
	}
	
	public static FlowSegment namedOutput(String name, FlowSegment... segment) {
		return new NamedOutput(name, Sequential.of(segment));
	}
	
	public static class DefaultFlowConnection implements FlowConnection {
		
		private final FlowSegment source;
		private final FlowSegment destination;
		private final String name;
		
		public static FlowConnection create(FlowSegment source, FlowSegment destination, String name) {
			return new DefaultFlowConnection(source, destination, name);
		}
		private DefaultFlowConnection(FlowSegment source, FlowSegment destination, String name) {
			this.source = source; this.destination = destination; this.name = name;
		}
		@Override
        public FlowSegment source() {
			return source;
		}
		@Override
        public FlowSegment destination() {
			return destination;
		}
		@Override
        public String name() {
			return name;
		}
		@Override
		public String toString() {
			if (name != null) {
				return format("FlowConnection(%s: %s -> %s)", name, source.inputName(), destination.inputName());
			} else {
				return format("FlowConnection(%s -> %s)", source.inputName(), destination.inputName());
			}
		}
	}
	
	public static abstract class AbstractFlowSegment implements FlowSegment {

		private String inputName;
		private String outputName;
		private String label;
		private final Collection<FlowSegment> start = new ArrayList<>();
		private final Collection<FlowSegment> end = new ArrayList<>();
		private final Collection<FlowConnection> connections = new ArrayList<>();
		
		protected AbstractFlowSegment start(Collection<? extends FlowSegment> value) {
			start.addAll(value);
			return this;
		}
		
		protected AbstractFlowSegment start(FlowSegment... value) {
			return start(Arrays.asList(value));
		}

		protected AbstractFlowSegment end(Collection<? extends FlowSegment> value) {
			end.addAll(value);
			return this;
		}
		
		protected AbstractFlowSegment end(FlowSegment... value) {
			return end(Arrays.asList(value));
		}
		
		protected AbstractFlowSegment connection(FlowSegment source, FlowSegment destination) {
			connections.add(DefaultFlowConnection.create(source, destination, null));
			return this;
		}
		
		protected AbstractFlowSegment connection(FlowSegment source, FlowSegment destination, String label) {
			connections.add(DefaultFlowConnection.create(source, destination, label));
			return this;
		}
		
		protected AbstractFlowSegment inputName(String value) {
			inputName = value;
			return this;
		}
		
		protected AbstractFlowSegment outputName(String value) {
			outputName = value;
			return this;
		}
		
		protected AbstractFlowSegment label(String value) {
			label = value;
			return this;
		}
		
		@Override
		public Collection<FlowSegment> sources() {
			return start;
		}
		
		@Override
		public Collection<FlowSegment> destinations() {
			return end;
		}
		
		@Override
		public Collection<FlowConnection> connections() {
			return connections;
		}

        @Override
		public boolean isNode() {
			return false;
		}
		
		public FlowNode node() {
			return null;
		}
		
		@Override
		public String inputName() {
			return inputName;
		}
		
		@Override
		public String outputName() {
			return outputName;
		}
		
		@Override
		public String label() {
			return label;
		}
		
		@Override
		public Collection<FlowSegment> segments() {
			Set<FlowSegment> set = new HashSet<FlowSegment>();
			for (FlowSegment source : sources()) {
				set.add(source);
			}
			for (FlowConnection pair : connections()) {
				set.add(pair.source());
				set.add(pair.destination());
			}
			for (FlowSegment destination : destinations()) {
				set.add(destination);
			}
			return set;
		}
		
		@Override
		public String toString() {
			List<String> stuff = new ArrayList<>();
			if (label != null) stuff.add(format("label=%s", label));
			if (inputName != null) stuff.add(format("input-name=%s", inputName));
			if (outputName != null) stuff.add(format("output-name=%s", outputName));
			return format("%s(%s)", getClass().getSimpleName(), join(", ", stuff));
		}
	}
	
	private static class NamedInput extends AbstractFlowSegment {
		public NamedInput(String name, FlowSegment segment) {
			start(segment).end(segment).inputName(name);
		}
	}
	
	private static class NamedOutput extends AbstractFlowSegment {
		public NamedOutput(String name, FlowSegment segment) {
			start(segment).end(segment).outputName(name);
		}
	}
	
	private static class Parallel extends AbstractFlowSegment {
		public Parallel(FlowSegment... segments) {
			checkArgument(segments.length > 0, "tried to create a parallel segment with 0 elements!");
			start(segments).end(segments);
		}
		@Override
		public String toString() {
		    return format("Parallel(%s)", segments());
		}
	}
	
	private static class Halt extends AbstractFlowSegment {
		// no connections to anything
	}
 	
	private static class Sequential extends AbstractFlowSegment {
		
		private final FlowSegment[] segments;
		
		public static Sequential of(Iterable<? extends FlowSegment> segments) {
			return new Sequential(Iterables.toArray(segments, FlowSegment.class));
		}

		public static Sequential of(FlowSegment... rest) {
			return new Sequential(rest);
		}
		
		public static Sequential of(FlowSegment first, FlowSegment... rest) {
			List<FlowSegment> list = new ArrayList<>(); 
			list.addAll(asList(first));
			list.addAll(asList(rest));
			return of(list);
		}
		
		private Sequential(FlowSegment... segments) {
			this.segments = segments;
			if (segments.length == 0) return;
			start(segments[0]).end(segments[segments.length - 1]);
			for (int i = 0; i < segments.length - 1; i++) {
				connection(segments[i], segments[i + 1]);
			}
		}

		@Override
		public String toString() {
			return format("%s(%s)", getClass().getSimpleName(), asList(segments));
		}
		
	}
	
	private static class Labelled extends AbstractFlowSegment {
		
		private final String label;
		private final FlowSegment segment;
		private final Collection<FlowSegment> coll;
		
		public Labelled(String label, FlowSegment segment) {
			this.label = label;
			this.segment = segment;
			this.coll = ImmutableList.of(segment);
		}

		@Override
		public Collection<FlowSegment> sources() {
			return coll;
		}

		@Override
		public Collection<FlowSegment> destinations() {
			return coll;
		}

		@Override
		public Collection<FlowConnection> connections() {
			return segment.connections();
		}

		@Override
		public String label() {
			return label;
		}

		@Override
		public Collection<FlowSegment> segments() {
			return coll;
		}
		
	}

}
