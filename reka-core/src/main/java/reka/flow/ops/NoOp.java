package reka.flow.ops;

import reka.flow.SimpleFlowOperation;

public class NoOp implements SimpleFlowOperation {

	public static final NoOp INSTANCE = new NoOp();
	
	private NoOp() { }
	
}
