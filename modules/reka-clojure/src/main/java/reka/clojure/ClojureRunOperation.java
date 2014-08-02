package reka.clojure;

import java.io.StringReader;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;
import clojure.lang.Compiler;

public class ClojureRunOperation implements SyncOperation {
	
	private final String script;
	
	public ClojureRunOperation(String script) {
		this.script = script;
	}

	@Override
	public MutableData call(MutableData data) {
		Compiler.load(new StringReader(script));
		return data;
	}
	
}