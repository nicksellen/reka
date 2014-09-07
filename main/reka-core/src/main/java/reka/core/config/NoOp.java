package reka.core.config;

import reka.api.flow.SimpleFlowOperation;

public class NoOp implements SimpleFlowOperation {

	public static final NoOp INSTANCE = new NoOp();
	
	private NoOp() { }
	
}
