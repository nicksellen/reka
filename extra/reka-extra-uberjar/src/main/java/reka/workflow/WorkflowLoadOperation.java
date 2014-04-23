package reka.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class WorkflowLoadOperation implements SyncOperation {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public MutableData call(MutableData data) {
		log.info("would load something");
		return data;
	}

}
