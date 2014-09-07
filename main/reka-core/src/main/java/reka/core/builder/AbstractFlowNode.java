package reka.core.builder;

import static java.lang.String.format;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.flow.FlowConnection;
import reka.api.flow.FlowDependency;
import reka.api.flow.FlowNode;
import reka.api.flow.FlowSegment;
import reka.core.data.memory.MutableMemoryData;

import com.google.common.collect.ImmutableList;

public class AbstractFlowNode implements FlowNode {
	
	private final MutableData meta = MutableMemoryData.create();
	
	private String name;
	private OperationSupplier<?> supplier;
	private FlowDependency embeddedFlowNode;
	private boolean subscribeable = false;
	private boolean isStart = false;
	private boolean isEnd = false;
	private boolean shouldUseAnotherThread = false;
	
	protected AbstractFlowNode embeddedFlowNode(FlowDependency embeddedFlowNode) {
		this.embeddedFlowNode = embeddedFlowNode;
		return this;
	}
	
	protected AbstractFlowNode isStart(boolean val) {
		isStart = val;
		return this;
	}

	protected AbstractFlowNode isEnd(boolean val) {
		isEnd = val;
		return this;
	}
	
	protected AbstractFlowNode name(String name) {
		this.name = name;
		return this;
	}
	
	protected AbstractFlowNode shouldUseAnotherThread(boolean val) {
		this.shouldUseAnotherThread = val;
		return this;
	}
	
	protected AbstractFlowNode supplier(OperationSupplier<?> supplier) {
		this.supplier = supplier;
		return this;
	}
	
	protected AbstractFlowNode subscribeable(boolean subscribable) {
		this.subscribeable = subscribable;
		return this;
	}
	
	public String inputName() {
		return null;
	}

	@Override
	public boolean hasOperationSupplier() {
		return supplier != null;
	}
	
	@Override
	public OperationSupplier<?> operationSupplier() {
		return supplier;
	}
	
	@Override
	public FlowNode node() {
		return this;
	}
	
	@Override
	public Collection<FlowSegment> sources() {
		return ImmutableList.<FlowSegment>of(this);
	}
	
	@Override
	public Collection<FlowSegment> destinations() {
		return ImmutableList.<FlowSegment>of(this);
	}
	
	@Override
	public Collection<FlowConnection> connections() {
		return Collections.emptyList();
	}
	
	@Override
	public boolean isNode() {
		return true;
	}
	
	@Override
	public Collection<FlowSegment> segments() {
		Set<FlowSegment> set = new HashSet<>();
		set.add(this);
		return set;
	}
	
	@Override
	public String label() {
		return name;
	}
	
	@Override
	public String outputName() {
		return null;
	}
	
	@Override
	public Data meta() {
		return meta;
	}
	
	@Override
	public String toString() {
	    return format("Node(%s, operation: %s)", name, supplier);
	}

	@Override
	public boolean hasEmbeddedFlow() {
		return embeddedFlowNode != null;
	}

	@Override
	public FlowDependency embeddedFlowNode() {
		return embeddedFlowNode;
	}

	@Override
	public boolean isSubscribeable() {
		return subscribeable;
	}

	@Override
	public boolean isStart() {
		return isStart;
	}

	@Override
	public boolean isEnd() {
		return isEnd;
	}

	@Override
	public boolean shouldUseAnotherThread() {
		return shouldUseAnotherThread;
	}
	

}