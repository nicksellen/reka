package reka.admin;

import static reka.api.Path.dots;
import static reka.core.builder.FlowSegments.async;
import static reka.util.Util.runtime;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.configurer.annotations.Conf;
import reka.core.util.StringWithVars;

public class RekaDeployConfigurer implements Supplier<FlowSegment> {

	private final ApplicationManager manager;
	
	private Path in;
	private Function<Data,String> filenameFn;
	private Function<Data,String> identityFn = (unused) -> UUID.randomUUID().toString();
	
	RekaDeployConfigurer(ApplicationManager manager) {
		this.manager = manager;
	}
	
	@Conf.At("in")
	public void in(String val) {
		in = dots(val);
	}
	
	@Conf.At("filename")
	public void filename(String val) {
		filenameFn = StringWithVars.compile(val);
	}
	
	@Conf.At("identity")
	public void identity(String val) {
		identityFn = StringWithVars.compile(val);
	}
	
	@Override
	public FlowSegment get() {
		if (in != null) {
			return async("deploy", () -> new RekaDeployFromContentOperation(manager, in));
		} else if (filenameFn != null) {
			return async("deploy", () -> new RekaDeployFromFileOperation(manager, filenameFn, identityFn));
		} else {
			throw runtime("must specify either 'in' or 'filename'");
		}
	}
	
}