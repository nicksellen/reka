package reka.core.setup;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import reka.api.IdentityStore;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.Operation;
import reka.api.run.OperationContext;
import reka.core.app.IdentityAndVersion;
import reka.core.setup.ModuleSetup.DoneCallback;

// wraps the full-on OperationSetup and only allows operations to be defined, which just see the store
public class ModuleOperationSetup {
	
	private final IdentityAndVersion idv;
	private final OperationSetup ops;
	
	public ModuleOperationSetup(IdentityAndVersion idv, OperationSetup ops) {
		this.idv = idv;
		this.ops = ops;
	}
	
	public ModuleOperationSetup run(String name, Consumer<ModuleSetupContext> c) {
		ops.add(name, ctx -> {
			return new Operation() {
				
				@Override
				public void call(MutableData data, OperationContext rctx) {
					c.accept(ctx);
				}
				
			};
		});
		return this;
	}

	public ModuleOperationSetup run(String name, BiConsumer<IdentityAndVersion, IdentityStore> c) {
		ops.add(name, ctx -> {
			return new Operation() {
				
				@Override
				public void call(MutableData data, OperationContext rctx) {
					c.accept(idv, ctx);
				}
				
			};
		});
		return this;
	}
	
	public ModuleOperationSetup runAsync(String name, BiConsumer<IdentityStore, DoneCallback> c) {
		ops.add(name, ctx -> {
			return AsyncOperation.create((data, rctx, res) -> c.accept(ctx, () -> res.done()));
		});
		return this;
	}
	
	public ModuleOperationSetup parallel(Consumer<ModuleOperationSetup> par) {
		ops.parallel(p -> {
			par.accept(new ModuleOperationSetup(idv, p));
		});
		return this;
	}
	
	public <T> ModuleOperationSetup eachParallel(Iterable<T> it, BiConsumer<T, ModuleOperationSetup> seq) {
		ops.eachParallel(it, (v, s) -> {
			seq.accept(v, new ModuleOperationSetup(idv, s));
		});
		return this;
	}
	
}