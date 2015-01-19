import static reka.api.Path.path;

import javax.inject.Inject;

import reka.api.Path;
import reka.core.module.Module;
import reka.core.module.ModuleDefinition;
import reka.net.NetModule;
import reka.net.NetServerManager;

public class GithubModule implements Module {
	
	private final NetServerManager server;
	
	@Inject
	public GithubModule(NetModule net) {
		this.server = net.server();
	}

	public Path base() {
		return path("github");
	}

	public void setup(ModuleDefinition module) {
		module.main(() -> new GithubConfigurer(server));
	}

}
