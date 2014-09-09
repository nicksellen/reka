package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.unwrap;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.EverythingSubscriber;
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
	public void run(MutableData data, OperationResult ctx) {
		
		String filename = filenameFn.apply(data);
		String identity = identityFn.apply(data);

		data.putString("identity", identity);
		
		File file = new File(filename);
		
		checkArgument(file.exists(), "file does not exist [%s]", filename);
		checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
		
		log.info("deploying {}", identity);
		
		manager.deploy(identity, FileSource.from(file), new EverythingSubscriber() {

			@Override
			public void ok(MutableData initializationData) {
				log.info("deploying {} ok", identity);
				data.putString("message", "created application!");
				ctx.done();
			}

			@Override
			public void halted() {
				log.info("deploying {} halt", identity);
				String msg = "failed to deploy application; initialization halted";
				log.debug(msg);
				data.putString("message", msg);
				ctx.error(new RuntimeException("halted!"));
			}

			@Override
			public void error(Data initializationData, Throwable t) {
				log.info("deploying {} error", identity);
				t = unwrap(t);
				log.error("failed to deploy application",  t);
				t.printStackTrace();
				ctx.error(t);
			}
			
		});
		
	}
}