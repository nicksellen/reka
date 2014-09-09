package reka.api.run;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface Operation extends SimpleFlowOperation {
	
	public void call(MutableData data);

	default AsyncOperation asAsync() {
		return AsyncOperation.create((data, ctx) -> {
			call(data);
			ctx.done();
		});
	}
	
}