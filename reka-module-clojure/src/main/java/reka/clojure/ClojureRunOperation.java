package reka.clojure;

import reka.clojure.env.ClojureEnv;
import reka.data.MutableData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;

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