package reka;

import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;

public class FlowTest {

	private final Data initial;
	private final Supplier<FlowSegment> run;
	private final Data expect;
	
	public FlowTest(Data initial, Supplier<FlowSegment> run, Data expect) {
		this.initial = initial;
		this.run = run;
		this.expect = expect;
	}
	
	public Data initial() {
		return initial;
	}
	
	public Supplier<FlowSegment> run() {
		return run;
	}
	
	public Data expect() {
		return expect;
	}

}
