package reka.test.content;

import org.junit.Test;

public class DefaultFlowTest {
	
	@Test
	public void test() throws InterruptedException {
		
		/*
		CountDownLatch latch = new CountDownLatch(1);
		
		FlowsBuilder builder = new FlowsBuilder();
		builder.add("main", seq(
			node("some node", () -> ((data) -> {
				log.debug("running some node");
				return data;
			}))),
			node("some other node", () -> ((data) -> {
				log.debug("running some other node");
				return data;
			})));
				
		Flows flows = builder.build();
		Flow flow = flows.flow("main");
		
		flow.prepare()
				.data(MemoryData.createMap())
				.complete((data) -> {
					log.debug("finished!");
					latch.countDown();
				}).run();
		
		if (latch.await(500, TimeUnit.MILLISECONDS)) {
			
		} else {
			fail("latch didn't countdown :(");
		}
		*/
	}

}
