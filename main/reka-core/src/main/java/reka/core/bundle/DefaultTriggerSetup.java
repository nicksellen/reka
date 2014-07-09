package reka.core.bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import reka.api.Path;

public class DefaultTriggerSetup implements SetupTrigger {
	
	private final String identity;
	private final Path applicationName;
	private final int applicationVersion;
	private final Set<Path> requiresFlows = new HashSet<>();
	private final List<Consumer<Registration>> registrationHandlers = new ArrayList<>();
	
	public DefaultTriggerSetup(String identity, Path applicationName, int applicationVersion) {
		this.identity = identity;
		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
	}
	
	@Override
	public Path applicationName() {
		return applicationName;
	}
	
	@Override
	public int applicationVersion() {
		return applicationVersion;
	}
	
	@Override
	public String identity() {
		return identity;
	}

	@Override
	public void requiresFlow(Path flowName) {
		// FIXME: namespacing?
		requiresFlows.add(flowName);
	}

	@Override
	public void requiresFlows(Collection<Path> flowNames) {
		// FIXME: namespacing?
		requiresFlows.addAll(flowNames);
	}

	@Override
	public void addRegistrationHandler(Consumer<Registration> startup) {
		registrationHandlers.add(startup);
	}
	
	public List<Consumer<Registration>> registrationHandlers() {
		return registrationHandlers;
	}

}
