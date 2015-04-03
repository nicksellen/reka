package reka.process;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.AsyncOperation;
import reka.flow.ops.OperationContext;

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