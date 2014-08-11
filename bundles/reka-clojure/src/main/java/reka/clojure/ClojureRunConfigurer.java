package reka.clojure;

import static reka.core.builder.FlowSegments.sync;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;

public class ClojureRunConfigurer implements Supplier<FlowSegment> {
	
	private final AtomicReference<ClojureEnv> runtimeRef;

	private String fn;
	
	public ClojureRunConfigurer(AtomicReference<ClojureEnv> runtimeRef) {
		this.runtimeRef = runtimeRef;
	}
	
	@Conf.Val
	public void fn(String val) {
		fn = val;
	}
	
	@Override
	public FlowSegment get() {
		return sync("run clojure", () -> new ClojureRunOperation(runtimeRef.get(), fn));
	}
	
}