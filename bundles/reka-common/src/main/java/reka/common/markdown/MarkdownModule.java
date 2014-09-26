package reka.common.markdown;

import static reka.api.Path.root;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import reka.core.setup.ModuleConfigurer;
import reka.core.setup.ModuleSetup;

public class MarkdownModule extends ModuleConfigurer {
	
	protected static final ThreadLocal<PegDownProcessor> md = ThreadLocal.withInitial(() -> new PegDownProcessor(Extensions.ALL));

	@Override
	public void setup(ModuleSetup module) {
		module.operation(root(), provider -> new MarkdownConfigurer());
	}

}
