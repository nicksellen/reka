package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.deleteRecursively;
import static reka.util.Util.runtime;
import static reka.util.Util.unwrap;
import static reka.util.Util.unzip;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.Identity;
import reka.api.Path;
import reka.api.content.Content;
import reka.api.content.types.BinaryContent;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;
import reka.config.FileSource;
import reka.core.app.Application;
import reka.core.app.manager.ApplicationManager;
import reka.core.app.manager.ApplicationManager.DeploySubscriber;
import reka.dirs.AppDirs;
import reka.dirs.BaseDirs;

public class RekaDeployOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final BaseDirs basedirs;
	private final Function<Data,Path> dataPathFn;
	private final Function<Data,Path> appPathFn;
	
	public RekaDeployOperation(ApplicationManager manager, BaseDirs basedirs, Function<Data, Path> dataPathFn, Function<Data,Path> appPathFn) {
		this.manager = manager;
		this.basedirs = basedirs;
		this.dataPathFn = dataPathFn;
		this.appPathFn = appPathFn;
	}
	
	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {

		Path dataPath = dataPathFn.apply(data);
		Path appPath = appPathFn.apply(data);

		data.putString("identity", appPath.slashes());

		int version = manager.nextVersion(appPath);
		AppDirs dirs = basedirs.resolve(appPath, version);

		Data val = data.at(dataPath);

		if (!val.isPresent()) throw runtime("no data at %s", dataPath.dots());
		if (!val.isContent()) throw runtime("not content at %s", dataPath.dots());
		if (!val.content().type().equals(Content.Type.BINARY)) throw runtime("must be binary content at %s", dataPath.dots());
		BinaryContent bc = (BinaryContent) val.content();
		if (!"application/zip".equals(bc.contentType())) throw runtime("must be application/zip content at %s", dataPath.dots());

		log.info("unpacking {} to {}", appPath.slashes(), dirs.app());

		dirs.mkdirs();
		deleteRecursively(dirs.app());
		
		unzip(bc.asBytes(), dirs.app());
		
		File appdir = dirs.app().toFile();
		File mainreka = dirs.app().resolve("main.reka").toFile();
		
		checkArgument(appdir.exists(), "app dir does not exist [%s]", appdir);
		checkArgument(appdir.isDirectory(), "app dir is not directory [%s]", appdir);
		checkArgument(mainreka.exists(), "main.reka does not exist");
		checkArgument(!mainreka.isDirectory(), "main.reka is a directory");
		
		log.info("deploying {}", appPath.slashes());
		
		manager.deploySource(appPath, -1, FileSource.from(mainreka), new DeploySubscriber() {

			@Override
			public void ok(Identity identity, int version, Application application) {
				log.info("deploying {} ok", identity);
				data.putString("message", "created application!");
				res.done();
				
				// delete a few old versions...
				for (int v = version - 3; v >= 0; v--) {
					deleteRecursively(basedirs.resolve(identity.path(), v).app());			
				}
				
			}

			@Override
			public void error(Identity identity, Throwable t) {
				t = unwrap(t);
				log.error("failed to deploy [{}] - {}", identity, t.getMessage());
				res.error(t);
				deleteRecursively(dirs.app());
			}
			
		});
		
	}
	
}