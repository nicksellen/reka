package reka.clojure;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

public class ClojureRunOperation implements Operation {
	
	private final ClojureEnv runtime;
	private final String namespacedFn;
	
	public ClojureRunOperation(ClojureEnv runtime, String namespacedFn) {
		this.runtime = runtime;
		this.namespacedFn = namespacedFn;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		runtime.run(namespacedFn, data.viewAsMap());
	}
	
}