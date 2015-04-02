package reka.postgres;

import static reka.api.Path.path;
import reka.api.Path;
import reka.api.flow.Flow;
import reka.core.data.memory.MutableMemoryData;

import com.impossibl.postgres.api.jdbc.PGNotificationListener;

public class NotifyFlow implements PGNotificationListener {

	private static final Path PROCESS_ID = path("process");
	private static final Path CHANNEL = path("channel");
	private static final Path PAYLOAD = path("payload");
	
	private final Flow flow;
	
	
	public NotifyFlow(Flow flow) {
		this.flow = flow;
	}
	
	@Override
	public void notification(int processId, String channel, String payload) {
		flow.prepare()
			.mutableData(MutableMemoryData.create()
			.putInt(PROCESS_ID, processId)
			.putString(CHANNEL, channel)
			.putString(PAYLOAD, payload)).run();
	}

}
