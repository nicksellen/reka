package reka.common.markdown;

import static reka.api.Path.path;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import reka.core.bundle.BundleConfigurer;

public class MarkdownBundle implements BundleConfigurer {
	
	protected static final ThreadLocal<PegDownProcessor> md = ThreadLocal.withInitial(() -> new PegDownProcessor(Extensions.ALL));

	@Override
	public void setup(BundleSetup bundle) {
		bundle.module(path("markdown"), () -> new MarkdownModule());
		bundle.converter(new MarkdownConverter());
	}

}
