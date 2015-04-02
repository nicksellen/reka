package reka.core.runtime.handlers.stateful;

public enum Lifecycle {
	ALWAYS_ACTIVE,		// we *always* go
	WAITING, 	// waiting to know what we're doing... (waiting for updates on other nodes OR arrivals)
	INACTIVE, 	// we've been deselected
	ACTIVE		// we've been selected for action
}