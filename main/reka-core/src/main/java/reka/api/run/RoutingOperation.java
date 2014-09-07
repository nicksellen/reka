package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;

public interface RoutingOperation extends FlowOperation {
	public void call(MutableData data, RouteCollector router);
}