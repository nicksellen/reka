package reka.elasticsearch;

import static reka.api.content.Contents.doubleValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;

public class JavascriptRhinoHelper {
	
	private static final Logger log = LoggerFactory.getLogger(JavascriptRhinoHelper.class);
    
	private static final ScriptableObject sealedSuperGlobalScope;

	static {
		Context context = Context.enter();
		try {
			sealedSuperGlobalScope = context.initStandardObjects(null, true);
			sealedSuperGlobalScope.sealObject();
		} finally {
			Context.exit();
		}
	}
	
	public static Script compileJavascript(String script) {
		Context context = Context.enter();
		try {
			return context.compileString(script, "", 1, null);
		} finally {
			Context.exit();
		}
	}

	public static Object runJavascriptWithParentScope(ScriptableObject base, Script script, Data data) {
		Context context = Context.enter();
		try {
			Scriptable scope = newScopeWithData(base, context, data);
			ScriptableObject.putProperty(scope, "exports", new NativeObject());
			script.exec(context, scope);
			return ScriptableObject.getProperty(scope, "exports");
		} finally {
			Context.exit();
		}
	}
	
	public static Object runJavascriptInScope(ScriptableObject scope, Script script) {
		Context context = Context.enter();
		try {
			ScriptableObject.putProperty(scope, "exports", new NativeObject());
			script.exec(context, scope);
			return ScriptableObject.getProperty(scope, "exports");
		} finally {
			Context.exit();
		}
	}
	
	public static Object runJavascript(Script script, Data data) {
		return runJavascriptWithParentScope(sealedSuperGlobalScope, script, data);
	}
	
	private static Scriptable newScope(ScriptableObject base, Context context) {
		Scriptable scope = context.newObject(base);
		scope.setPrototype(base);
		if (base == sealedSuperGlobalScope) scope.setParentScope(null);
		return scope;
	}
	
	private static Scriptable newScopeWithData(ScriptableObject base, Context context, Data data) {
		return addDataToScope(context, newScope(base, context), data);
	}
	
	private static Scriptable addDataToScope(Context context, Scriptable scope, Data data) {
		
		// TODO make this not use asMap()
		for (Entry<String, Object> entry : data.toMap().entrySet()) {
			ScriptableObject.putProperty(scope, entry.getKey(), javaToJavascript(context, scope, entry.getValue()));
		}
		return scope;
	}

	@SuppressWarnings("unchecked")
	private static Object javaToJavascript(Context context, Scriptable scope, Object object) {
		if (object instanceof Map) {
			Scriptable map = context.newObject(scope);
			for (Entry<String, Object> entry : ((Map<String, Object>) object).entrySet()) {
				ScriptableObject.putProperty(map, entry.getKey(), 
						javaToJavascript(context, scope, entry.getValue()));
			}
			return map;
		} else if (object instanceof List) {
			List<?> list = (List<?>) object;
			int length = list.size();
			Scriptable array = context.newArray(scope, length);
			for (int i = 0; i < length; i++) {
				ScriptableObject.putProperty(array, i, javaToJavascript(context, array, list.get(i)));
			}
			return array;
		} else {
			return object;
		}
	}

	public static void addToData(Object o, MutableData data) {
		addObjectToData(data, Path.root(), o);
	}
	
	public static void addObjectToData(MutableData data, Path path, Object o) {
		if (o instanceof NativeObject) {
			NativeObject nobj = (NativeObject) o;
			for (Object keyObject : ScriptableObject.getPropertyIds(nobj)) {
				String key = keyObject.toString();
				addObjectToData(data, path.add(key), ScriptableObject.getProperty(nobj, key));
			}
		} else if (o instanceof NativeArray) {
			NativeArray nary = (NativeArray) o;
			for (int i = 0; i < nary.getLength(); i++) {
				Object aryObj = ScriptableObject.getProperty(nary, i);
				addObjectToData(data, path.add(i), aryObj);
			}
		} else if (o instanceof ConsString) {
			data.putString(path, ((ConsString) o).toString());
		} else if (o instanceof String) {
			data.putString(path, (String) o);
		} else if (o instanceof Integer) {
			data.putInt(path, (Integer) o);
		} else if (o instanceof Long) {
			data.putLong(path, (Long) o);
		} else if (o instanceof Double) {
			data.put(path, doubleValue((Double) o));
		} else if (o instanceof Boolean) {
			data.putBool(path, (Boolean) o);
		} else if (o == null) {
			data.putNull(path);
		} else {
			log.debug("WARN we didn't handle data of type {} from javascript\n", o.getClass());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String,Object> jsToMap(NativeObject so) {
		return (Map<String,Object>) convert((NativeObject) so);
	}
	
	private static Object convert(Object o) {
		if (o instanceof NativeObject) {
			Map<String,Object> mp = new HashMap<>();
			NativeObject no = (NativeObject) o;
			for (Object keyObject : ScriptableObject.getPropertyIds(no)) {
				String key = keyObject.toString();
				mp.put(key, convert(ScriptableObject.getProperty(no, key)));
			}
			return mp;
		} else if (o instanceof NativeArray) {
			List<Object> items = new ArrayList<>();
			NativeArray ary = (NativeArray) o;
			for (int i = 0; i < ary.getLength(); i++) {
				Object aryObj = ScriptableObject.getProperty(ary, i);
				items.add(convert(aryObj));
			}
			return items;
		} else {
			return o;
		}
	}
	
}
