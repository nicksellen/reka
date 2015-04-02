package reka.process;

import java.util.function.Function;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

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