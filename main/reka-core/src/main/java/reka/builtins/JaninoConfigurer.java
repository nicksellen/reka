package reka.builtins;

import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.util.Util.unchecked;

import java.lang.reflect.Method;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;

import reka.api.data.MutableData;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationSetup;
import reka.nashorn.OperationConfigurer;

public class JaninoConfigurer implements OperationConfigurer {

	private Method method;
	
	@Conf.Config
	public void config(Config config) {
		checkConfig(config.hasDocument(), "must have a document");
		String script = config.documentContentAsString();
		try {
			ScriptEvaluator se = new ScriptEvaluator();
	        se.setParameters(new String[]{ "data" }, new Class[]{ MutableData.class });
			se.cook(script);
			method = se.getMethod();
		} catch (CompileException e) {
			throw unchecked(e);
		}
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", store -> new JaninoOperation(method));
	}

}
