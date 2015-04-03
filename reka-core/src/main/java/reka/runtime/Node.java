package reka.runtime;

import reka.runtime.handlers.ActionHandler;

public interface Node extends ActionHandler, FailureHandler {
    int id();
	String name();
}
