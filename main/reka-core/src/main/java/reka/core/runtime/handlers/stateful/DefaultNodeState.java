package reka.core.runtime.handlers.stateful;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import reka.api.data.MutableData;

public class DefaultNodeState implements NodeState {
	
	private final List<MutableData> data = new ArrayList<>();
	
	private int initial;
	private int remaining;
	private boolean initialized = false;

	private Lifecycle lifecycle = Lifecycle.WAITING;
	private boolean atLeastOneThingArrived;
		
	@Override
	public NodeState initialize(int value) {
		if (initialized) {
			checkState(initial == value, "re-initialize with a different value !?");
		} else {
			this.initial = value;
			this.remaining = value;
			initialized = true;
		}
		return this;
	}
	
	@Override
    public void decrement() {
		checkState(initialized, "you must initialize the state");
		remaining -= 1;
		checkState(remaining >= 0, "remaining must not go below zero");
		if (remaining == 0) {
			if (atLeastOneThingArrived) {
				lifecycle = Lifecycle.ACTIVE;
			} else {
				lifecycle = Lifecycle.INACTIVE;
			}
		}
	}
	
	@Override
    public Lifecycle lifecycle() {
		return lifecycle;
	}
	
	@Override
    public NodeState arrived(MutableData data) {
		this.data.add(data);
		atLeastOneThingArrived = true;
		return this;
	}
	
	@Override
    public Iterable<MutableData> data() {
		return data;
	}
	
	@Override
	public String toString() {
		return format("%s(remaining: %s, readyToRun: %s, lifecycle: %s)", getClass().getSimpleName(), remaining, atLeastOneThingArrived, lifecycle);
	}
	
	
}