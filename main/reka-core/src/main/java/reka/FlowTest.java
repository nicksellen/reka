package reka;

import java.util.List;
import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;

public class FlowTest {

	private final Supplier<FlowSegment> run;
	private final List<FlowTestCase> cases;
	
	public static class FlowTestCase {
		
		private final String name;
		private final Data initial;
		private final Data expect;
		
		public FlowTestCase(String name, Data initial, Data expect) {
			this.name = name;
			this.initial = initial;
			this.expect = expect;
		}
		
		public String name() {
			return name;
		}
		
		public Data initial() {
			return initial;
		}
		
		public Data expect() {
			return expect;
		}
		
	}
	
	public FlowTest(Supplier<FlowSegment> run, List<FlowTestCase> cases) {
		this.run = run;
		this.cases = cases;
	}
	
	public Supplier<FlowSegment> run() {
		return run;
	}
	
	public List<FlowTestCase> cases() {
		return cases;
	}

}
