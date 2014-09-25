package reka.core.setup;

import reka.api.flow.Flow;

public class SingleFlowRegistration extends BaseRegistration {

	private final Flow flow;
	
	SingleFlowRegistration(BaseRegistration base, Flow flow) {
		super(base.applicationVersion, base.identity, base.store, base.network, base.undeployConsumers, base.pauseConsumers, base.resumeConsumers);
		this.flow = flow;
	}
	
	public Flow flow() {
		return flow;
	}
	
}