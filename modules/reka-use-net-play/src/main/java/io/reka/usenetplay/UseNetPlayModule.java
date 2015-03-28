package io.reka.usenetplay;

import static reka.api.Path.slashes;

import javax.inject.Inject;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.net.NetModule;
import reka.net.NetManager;

public class UseNetPlayModule implements Module {

	private final NetManager server;
	
	@Inject
	public UseNetPlayModule(NetModule net) {
		this.server = net.server();
	}
	
	@Override
	public Path base() {
		return slashes("usenetplay");
	}

	@Override
	public void setup(ModuleDefinition module) {
		module.main(() -> new UseNetPlayConfigurer(server));
	}

	
	
}
