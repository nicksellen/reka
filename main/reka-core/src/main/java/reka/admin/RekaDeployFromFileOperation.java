package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.unwrap;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Application;
import reka.ApplicationManager;
import reka.ApplicationManager.DeploySubscriber;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.config.FileSource;

public class RekaDeployFromFileOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Function<Data,String> filenameFn, identityFn;
	
	public RekaDeployFromFileOperation(ApplicationManager manager, Function<Data,String> filenameFn, Function<Data,String> identityFn) {
		this.manager = manager;
		this.filenameFn = filenameFn;
		this.identityFn = identityFn;
	}
	
	@Override
	public void call(MutableData data, OperationResult ctx) {
		
		String filename = filenameFn.apply(data);
		String identity = identityFn.apply(data);

		data.putString("identity", identity);
		
		File file = new File(filename);
		
		checkArgument(file.exists(), "file does not exist [%s]", filename);
		checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
		
		log.info("deploying {}", identity);
		
		manager.deploySource(identity, FileSource.from(file), new DeploySubscriber() {

			@Override
			public void ok(String identity, int version, Application application) {
				log.info("deploying {} ok", identity);
				data.putString("message", "created application!");
				ctx.done();
			}

			@Override
			public void error(String identity, Throwable t) {
				t = unwrap(t);
				log.error("failed to deploy [{}] - {}", identity, t.getMessage());
				ctx.error(t);
			}
			
		});
		
	}
}