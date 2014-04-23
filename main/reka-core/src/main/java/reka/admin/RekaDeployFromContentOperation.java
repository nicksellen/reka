package reka.admin;

import static java.lang.String.format;
import static reka.util.Util.unwrap;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.ApplicationManager;
import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.EverythingSubscriber;
import reka.config.StringSource;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class RekaDeployFromContentOperation implements AsyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final ApplicationManager manager;
	private final Path in;
	
	public RekaDeployFromContentOperation(ApplicationManager manager, Path in) {
		this.manager = manager;
		this.in = in;
	}
	
	@Override
	public ListenableFuture<MutableData> call(MutableData data) {

		SettableFuture<MutableData> result = SettableFuture.create();
		
		String identity = UUID.randomUUID().toString();
		
		String configString = UseReka.getConfigStringFromData(data, in);
		
		manager.deploy(identity, StringSource.from(configString), new EverythingSubscriber() {

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
				String msg = format("failed to deploy application; error [%s]", t.getMessage());
				log.debug(msg);
				t.printStackTrace();
				result.setException(t);
			}
			
		});
		
		data.putString("identity", identity);
		
		return result;
		
		
	}
}