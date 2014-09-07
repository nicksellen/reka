package reka.rhino;

import static reka.rhino.RhinoHelper.addObjectToData;
import static reka.rhino.RhinoHelper.compileJavascript;
import static reka.rhino.RhinoHelper.runJavascript;

import org.mozilla.javascript.Script;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.Operation;

public class Rhino implements Operation {

	private final Script script;
	private final Path out;
	
	public Rhino(String code, Path out) {
		script = compileJavascript(code);
		this.out = out;
	}
	
	@Override
	public MutableData call(MutableData data) {
		addObjectToData(data, out, runJavascript(script, data));
		return data;
	}
	
}
