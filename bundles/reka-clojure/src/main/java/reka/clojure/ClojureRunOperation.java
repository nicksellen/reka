package reka.clojure;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import reka.clojure.UseClojure.ClojureEnv;

public class ClojureRunOperation implements SyncOperation {
	
	private final ClojureEnv runtime;
	private final String ns;
	private final String fn;
	
	public ClojureRunOperation(ClojureEnv runtime, String val) {
		this.runtime = runtime;
		String[] f = val.split("\\/");
		ns = f[0];
		fn = f[1];
	}

	@Override
	public MutableData call(MutableData data) {
		runtime.run(ns, fn, data.viewAsMap());
//		Compiler.load(new StringReader(script));
		return data;
	}
	
}