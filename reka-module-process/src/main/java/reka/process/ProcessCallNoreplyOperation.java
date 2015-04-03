package reka.process;

import java.util.function.Function;

import reka.data.Data;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

public class ProcessCallNoreplyOperation implements Operation {

	private final ProcessManager manager;
	private final Function<Data,String> lineFn;
	
	public ProcessCallNoreplyOperation(ProcessManager manager, Function<Data,String> lineFn) {
		this.manager = manager;
		this.lineFn = lineFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx) {
		manager.send(lineFn.apply(data));
	}
	
}