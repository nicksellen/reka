package reka.core.runtime;

import reka.core.runtime.handlers.ActionHandler;

public interface Node extends ActionHandler, FailureHandler {
    int id();
	String name();
}
