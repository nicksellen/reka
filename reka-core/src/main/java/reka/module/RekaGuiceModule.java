package reka.module;

import java.util.Collection;

import com.google.inject.AbstractModule;

public class RekaGuiceModule extends AbstractModule {

	private final Collection<Class<?>> classes;
	
	public RekaGuiceModule(Collection<Class<?>> classes) {
		this.classes = classes;
	}
	
	@Override
	protected void configure() {
		classes.forEach(klass -> {
			// TODO: this totally doesn't do what I want, need to use OSGi really...
			requireBinding(klass);
		});
	}

}
