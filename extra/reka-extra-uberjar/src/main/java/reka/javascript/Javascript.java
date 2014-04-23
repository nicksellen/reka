package reka.javascript;

import static reka.javascript.JavascriptRhinoHelper.addObjectToData;
import static reka.javascript.JavascriptRhinoHelper.compileJavascript;
import static reka.javascript.JavascriptRhinoHelper.runJavascript;

import org.mozilla.javascript.Script;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class Javascript implements SyncOperation {

	private final Script script;
	private final Path out;
	
	public Javascript(String code, Path out) {
		script = compileJavascript(code);
		this.out = out;
	}
	
	@Override
	public MutableData call(MutableData data) {
		addObjectToData(data, out, runJavascript(script, data));
		return data;
	}
	
}
