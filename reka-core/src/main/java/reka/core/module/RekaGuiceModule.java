package reka.core.module;

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
			System.out.printf("GUICE requireBinding %s\n", klass.getName());
			requireBinding(klass);
		});
	}

}
