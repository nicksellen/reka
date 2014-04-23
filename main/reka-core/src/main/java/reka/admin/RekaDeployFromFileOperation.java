package reka.admin;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.util.Util.unwrap;

import java.io.File;
import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.EverythingSubscriber;
import reka.config.FileSource;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class RekaDeployFromFileOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Function<Data,String> filenameFn;
	
	public RekaDeployFromFileOperation(ApplicationManager manager, Function<Data,String> filenameFn) {
		this.manager = manager;
		this.filenameFn = filenameFn;
	}
	
	@Override
	public ListenableFuture<MutableData> call(MutableData data) {

		SettableFuture<MutableData> result = SettableFuture.create();
		
		String identity = UUID.randomUUID().toString();
		
		String filename = filenameFn.apply(data);
		
		File file = new File(filename);
		
		checkArgument(file.exists(), "file does not exist [%s]", filename);
		checkArgument(!file.isDirectory(), "path is a directory [%s]", filename);
		
		manager.deploy(identity, FileSource.from(file), new EverythingSubscriber() {

			@Override
			public void ok(MutableData initializationData) {
				result.set(data.putString("message", "created application!"));
			}

			@Override
			public void halted() {
				String msg = "failed to deploy application; initialization halted";
				log.debug(msg);
				data.putString("message", msg);
				result.set(data);
			}

			@Override
			public void error(Data initializationData, Throwable t) {
				t = unwrap(t);
				log.error("failed to deploy application",  t);
				t.printStackTrace();
				result.setException(t);
			}
			
		});
		
		data.putString("identity", identity);
		
		return result;
		
		
	}
}