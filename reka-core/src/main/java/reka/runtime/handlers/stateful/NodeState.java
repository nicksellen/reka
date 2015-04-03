package reka.runtime.handlers.stateful;

import reka.data.MutableData;

public interface NodeState {
	NodeState initialize(int remaining);
    void decrement();
    Lifecycle lifecycle();
    NodeState arrived(MutableData data);
    Iterable<MutableData> data();
}
