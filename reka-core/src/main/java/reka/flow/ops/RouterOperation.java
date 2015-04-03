package reka.flow.ops;

import reka.data.MutableData;
import reka.flow.FlowOperation;

public interface RouterOperation extends FlowOperation {
	void call(MutableData data, RouteCollector router);
}