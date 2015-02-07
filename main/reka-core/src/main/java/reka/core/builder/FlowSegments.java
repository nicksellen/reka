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

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowConnection;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;
import reka.api.run.RouteKey;
import reka.core.config.NoOp;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class FlowSegments extends AbstractFlowNode {
	
	public static FlowNode noop() {
		return OperationFlowNode.noop(); //(name, () -> NoOp.INSTANCE);
	}
	
	public static FlowSegment seq(Collection<? extends FlowSegment> segments) {
		return Sequential.of(segments);
	}

	public static FlowSegment seq(FlowSegment first, FlowSegment... rest) {
		return Sequential.of(first, rest);
	}
	
	public static FlowSegment parallel(FlowSegment first, FlowSegment... rest) {
		List<FlowSegment> list = new ArrayList<>(); 
		list.addAll(asList(first));
		list.addAll(asList(rest));
		return createParallelSegment(list);
	}
	
	public static FlowSegment createHalt() {
		return new Halt();
	}

	public static FlowSegment createParallelSegment(Collection<? extends FlowSegment> items) {
	    checkArgument(!items.isEmpty(), "a parallel segment with no items doesn't make sense");
		return new Parallel(items.toArray(new FlowSegment[items.size()]));
	}
	
	public static FlowSegment embeddedFlow(String embeddedFlowName) {
		return new EmbeddedFlowNode(embeddedFlowName, embeddedFlowName);
	}

	public static FlowNode createStartNode(String name) {
		return OperationFlowNode.node(name, () -> NoOp.INSTANCE).isStart(true);
	}
	
	public static FlowNode createEndNode(String name) {
		return OperationFlowNode.node(name, () -> NoOp.INSTANCE).isEnd(true);
	}
	
	public static FlowNode createSubscribeableEndNode(String name) {
		return new EndFlowNode(name);
	}
	
	public static FlowSegment createLabelSegment(String label, FlowSegment... segment) {
		return new Labelled(label, Sequential.of(segment));
	}

	public static FlowSegment createMetaSegment(FlowSegment segment, Data meta) {
		return new Meta(segment, meta);
	}
	
	public static class Meta extends AbstractFlowSegment {

		public Meta(FlowSegment segment, Data other) {
			mutableMeta().merge(other);
			start(segment).end(segment);
		}
		
	}
	
	public static FlowSegment createNamedInputSegment(RouteKey key, FlowSegment... segments) {
		return new NamedInput(key, Sequential.of(segments));
	}
	
	public static class DefaultFlowConnection implements FlowConnection {
		
		private final FlowSegment source;
		private final FlowSegment destination;
		
		public static FlowConnection create(FlowSegment source, FlowSegment destination) {
			return new DefaultFlowConnection(source, destination);
		}
		
		private DefaultFlowConnection(FlowSegment source, FlowSegment destination) {
			this.source = source;
			this.destination = destination;
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
		public String toString() {
			return format("FlowConnection(%s -> %s)", source.key(), destination.key());
		}
	}
	
	public static abstract class AbstractFlowSegment implements FlowSegment {

		private final MutableData meta = MutableMemoryData.create();
		
		private RouteKey inputName;
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
			connections.add(DefaultFlowConnection.create(source, destination));
			return this;
		}
		
		protected MutableData mutableMeta() {
			return meta;
		}
		
		protected AbstractFlowSegment inputName(RouteKey value) {
			inputName = value;
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
		public RouteKey key() {
			return inputName;
		}
		
		@Override
		public String label() {
			return label;
		}
		
		@Override
		public Data meta() {
			return meta.immutable();
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
			return format("%s(%s)", getClass().getSimpleName(), join(", ", stuff));
		}
	}
	
	private static class NamedInput extends AbstractFlowSegment {
		public NamedInput(RouteKey key, FlowSegment segment) {
			start(segment).end(segment).inputName(key);
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
