package reka.builtins;

import static reka.util.Util.unchecked;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import reka.api.data.MutableData;
import reka.api.run.Operation;

public class JaninoOperation implements Operation {
	
	private final Method method;
	
	public JaninoOperation(Method method) {
		this.method = method;
	}

	@Override
	public void call(MutableData data) {
		try {
			method.invoke(null, data);
		} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw unchecked(e);
		}
	}

}
