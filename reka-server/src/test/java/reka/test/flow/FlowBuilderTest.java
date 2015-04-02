package reka.test.flow;

import reka.api.data.MutableData;
import reka.api.run.Operation;
import reka.api.run.OperationContext;

public class FlowBuilderTest {


	public static class MessageWriter implements Operation {

		@SuppressWarnings("unused")
		private final String message;

		public MessageWriter(String message) {
			this.message = message;
		}

		@Override
		public void call(MutableData data, OperationContext ctx) {
			//System.out.log.debug("ran [{}]\n", message);

			// System.out.log.debug("js.result: [{}]\n",
			// data.get(dots("js.result")));
		}

	}

}