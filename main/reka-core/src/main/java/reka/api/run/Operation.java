package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface Operation extends SimpleFlowOperation {
	
	public void call(MutableData data, OperationContext ctx);

	default AsyncOperation asAsync() {
		return AsyncOperation.create((data, ctx, res) -> {
			call(data, ctx);
			res.done();
		});
	}
	
}