package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.unwrap;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.AppDirs;
import reka.Application;
import reka.ApplicationManager;
import reka.ApplicationManager.DeploySubscriber;
import reka.BaseDirs;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.config.FileSource;

public class RekaDeployFromUnpackOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final BaseDirs basedirs;
	private final Function<Data,String> identityFn;
	
	public RekaDeployFromUnpackOperation(ApplicationManager manager, BaseDirs dirs, Function<Data,String> identityFn) {
		this.manager = manager;
		this.basedirs = dirs;
		this.identityFn = identityFn;
	}
	
	@Override
	public void call(MutableData data, OperationResult ctx) {
		
		String identity = identityFn.apply(data);

		data.putString("identity", identity);
		
		AppDirs dirs = basedirs.resolve(identity);
		
		File appdir = dirs.app().toFile();
		File mainreka = dirs.app().resolve("main.reka").toFile();
		
		checkArgument(appdir.exists(), "app dir does not exist [%s]", appdir);
		checkArgument(appdir.isDirectory(), "app dir is not directory [%s]", appdir);
		checkArgument(mainreka.exists(), "main.reka does not exist");
		checkArgument(!mainreka.isDirectory(), "main.reka is a directory");
		
		log.info("deploying {}", identity);
		
		manager.deploySource(identity, FileSource.from(mainreka), new DeploySubscriber() {

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
				dirs.delete();
			}
			
		});
		
	}
}