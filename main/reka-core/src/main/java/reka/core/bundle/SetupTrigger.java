package reka.core.bundle;

import java.util.Collection;
import java.util.function.Consumer;

import reka.api.Path;

public interface SetupTrigger {
	
	String identity();
	Path applicationName();
	int applicationVersion();
	
	void requiresFlow(Path name);
	void requiresFlows(Collection<Path> names);
	
	void addRegistrationHandler(Consumer<Registration> constructed);
	
}