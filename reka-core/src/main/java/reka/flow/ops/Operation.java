package reka.flow.ops;

import reka.data.MutableData;
import reka.flow.SimpleFlowOperation;

public interface Operation extends SimpleFlowOperation {
	
	public void call(MutableData data, OperationContext ctx);

	default AsyncOperation asAsync() {
		return AsyncOperation.create((data, ctx, res) -> {
			call(data, ctx);
			res.done();
		});
	}
	
}