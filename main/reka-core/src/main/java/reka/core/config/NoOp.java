package reka.core.config;

import reka.api.flow.FlowOperation;

public class NoOp implements FlowOperation {

	public static final NoOp INSTANCE = new NoOp();
	
	private NoOp() { }
	
}
