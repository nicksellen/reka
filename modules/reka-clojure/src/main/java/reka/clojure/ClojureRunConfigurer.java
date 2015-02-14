package reka.clojure;

import static reka.clojure.ClojureConfigurer.CLOJURE_ENV;
import reka.config.configurer.annotations.Conf;
import reka.core.setup.OperationConfigurer;
import reka.core.setup.OperationSetup;

public class ClojureRunConfigurer implements OperationConfigurer {

	private String fn;
	
	@Conf.Val
	public void fn(String val) {
		fn = val;
	}
	
	@Override
	public void setup(OperationSetup ops) {
		ops.add("run", store -> new ClojureRunOperation(store.get(CLOJURE_ENV), fn));
	}
	
}