package reka.module.setup;

import reka.flow.Flow;

public class SingleTriggerFlow {

	private final Flow flow;
	
	SingleTriggerFlow(Flow flow) {
		this.flow = flow;
	}
	
	public Flow flow() {
		return flow;
	}
	
}