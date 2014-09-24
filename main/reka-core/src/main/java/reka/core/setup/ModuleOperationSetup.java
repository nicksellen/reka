package reka.core.setup;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import reka.api.IdentityStore;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.core.setup.ModuleSetup.DoneCallback;

// wraps the full-on OperationSetup and only allows operations to be defined, which just see the store
public class ModuleOperationSetup {
	
	private final OperationSetup ops;
	
	public ModuleOperationSetup(OperationSetup ops) {
		this.ops = ops;
	}
	
	public ModuleOperationSetup run(String name, Consumer<IdentityStore> c) {
		ops.add(name, store -> {
			return new Operation() {
				
				@Override
				public void call(MutableData data) {
					c.accept(store);
				}
				
			};
		});
		return this;
	}
	
	public ModuleOperationSetup runAsync(String name, BiConsumer<IdentityStore, DoneCallback> c) {
		ops.add(name, store -> {
			return AsyncOperation.create((data, ctx) -> c.accept(store, () -> ctx.done()));
		});
		return this;
	}
	
	public ModuleOperationSetup parallel(Consumer<ModuleOperationSetup> par) {
		ops.parallel(p -> {
			par.accept(new ModuleOperationSetup(p));
		});
		return this;
	}
	
	public <T> ModuleOperationSetup eachParallel(Iterable<T> it, BiConsumer<T, ModuleOperationSetup> seq) {
		ops.eachParallel(it, (v, s) -> {
			seq.accept(v, new ModuleOperationSetup(s));
		});
		return this;
	}
	
}