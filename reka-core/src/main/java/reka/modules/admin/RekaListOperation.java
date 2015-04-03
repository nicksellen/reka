package reka.modules.admin;

import reka.app.Application;
import reka.app.manager.ApplicationManager;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.flow.ops.Operation;
import reka.flow.ops.OperationContext;
import reka.identity.Identity;
import reka.util.Path;



public class RekaListOperation implements Operation {
	
	private final ApplicationManager manager;
	private final Path out;
	
	public RekaListOperation(ApplicationManager manager, Path out) {
		this.manager = manager;
		this.out = out;
	}

	@Override
	public void call(MutableData data, OperationContext ctx) {
		data.putList(out, list -> {
			manager.forEach(e -> {
				Identity identity = e.getKey();
				Application app = e.getValue();	
				MutableData item = MutableMemoryData.create();
				item.putString("id", identity.name());
				AdminUtils.putAppDetails(item.createMapAt("app"), app, manager.statusFor(identity));
				list.add(item);
			});
		});
	}

}
