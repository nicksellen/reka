package reka.core.bundle;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import reka.api.Path;
import reka.api.data.Data;
import reka.core.builder.Flows;

public interface SetupTrigger {
	
	String identity();
	Path applicationName();
	//int version();
	
	void requiresFlow(Path name);
	void requiresFlows(Collection<Path> names);
	
	void onStart(Consumer<Contructed> constructed);
	
	static interface Contructed {
		Flows flows();
		void onUndeploy(IntConsumer run);
		void register(int port, String protocol, Data data);
	}
	
}