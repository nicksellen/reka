package reka.process;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;

public class ProcessCallOperation implements AsyncOperation {

	private final ProcessManager manager;
	private final Function<Data,String> lineFn;
	
	public ProcessCallOperation(ProcessManager manager, Function<Data,String> lineFn) {
		this.manager = manager;
		this.lineFn = lineFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {
		manager.send(lineFn.apply(data), output -> {
			data.putString("out", output);
			res.done();
		});
	}
	
}