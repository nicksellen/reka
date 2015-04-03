package reka.markdown;

import static reka.api.Path.root;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import reka.module.setup.AppSetup;
import reka.module.setup.ModuleConfigurer;

public class MarkdownModuleConfigurer extends ModuleConfigurer {
	
	protected static final ThreadLocal<PegDownProcessor> md = ThreadLocal.withInitial(() -> new PegDownProcessor(Extensions.ALL));

	@Override
	public void setup(AppSetup module) {
		module.defineOperation(root(), provider -> new MarkdownConfigurer());
	}

}
