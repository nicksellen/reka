package reka.rhino;

import static reka.rhino.RhinoHelper.addObjectToData;
import static reka.rhino.RhinoHelper.runJavascriptWithParentScope;

import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

import reka.api.Path;
import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class RhinoWithScope implements SyncOperation {

	private final ScriptableObject scope;
	private final Script script;
	private final Path out;
	
	public RhinoWithScope(ScriptableObject scope, Script script, Path out) {
		this.scope = scope;
		this.script = script;
		this.out = out;
	}
	
	@Override
	public MutableData call(MutableData data) {
		addObjectToData(data, out, runJavascriptWithParentScope(scope, script, data));
		return data;
	}
	
}
