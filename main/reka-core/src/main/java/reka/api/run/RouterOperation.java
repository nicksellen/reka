package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.FlowOperation;

public interface RouterOperation extends FlowOperation {
	void call(MutableData data, RouteCollector router);
}