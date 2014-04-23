package reka.test.flow;

import reka.api.data.MutableData;
import reka.api.run.SyncOperation;

public class FlowBuilderTest {


	public static class MessageWriter implements SyncOperation {

		@SuppressWarnings("unused")
		private final String message;

		public MessageWriter(String message) {
			this.message = message;
		}

		@Override
		public MutableData call(MutableData data) {
			//System.out.log.debug("ran [{}]\n", message);

			// System.out.log.debug("js.result: [{}]\n",
			// data.get(dots("js.result")));
			return data;
		}

	}

}
