package reka.clojure;

import static reka.core.builder.FlowSegments.sync;

import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.configurer.annotations.Conf;

public class ClojureRunConfigurer implements Supplier<FlowSegment> {

	private String script;
	
	@Conf.Config
	public void config(Config config) {
		if (config.hasDocument()) {
			script = config.documentContentAsString();
		}
	}
	
	@Override
	public FlowSegment get() {
		return sync("run clojure", () -> new ClojureRunOperation(script));
	}
	
}