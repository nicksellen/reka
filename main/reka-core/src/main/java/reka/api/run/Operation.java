package reka.api.run;

import java.util.concurrent.Executor;

import reka.api.data.MutableData;
import reka.api.flow.SimpleFlowOperation;

public interface Operation extends SimpleFlowOperation {
	
	public void call(MutableData data);
	
	default public AsyncOperation async(Executor executor) {
		return AsyncOperation.create((data, ctx) -> {
			executor.execute(() -> {
				call(data);
				ctx.end();
			});
		});
	}
	
}