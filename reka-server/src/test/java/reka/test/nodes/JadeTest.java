package reka.test.nodes;


public class JadeTest {

	/* wait until I can work out what to do with building nodes outside of a flow context
	 * (perhaps there are two types of nodes, simple ones and ones that really do need the flow object)
	
	@Test
	public void test() {
		MutableContentStore config = MemoryContentStore.createMutableMap();
		
		config.put(path("jade", "template"), utf8("h1 A Nice Heading for #{name}"));
		
		ConfigurationService service = new ConfigurationService();
		
		final Jade jade = service.configurerFor(JadeBuilder.class)
			.create(config.contentStoreAt(path("jade"))).build();

		MutableContentStore data = MemoryContentStore.createMutableMap();
		
		data.put(path("name"), utf8("peter"));
		data.merge(jade.call(data));
		assertThat(data.get(Response.CONTENT).asUTF8(), equalTo("<h1>A Nice Heading for peter</h1>"));
		log.debug("output: {}\n", data.get(Response.CONTENT));
		

		data.put(path("name"), utf8("jane"));
		data.merge(jade.call(data));
		assertThat(data.get(Response.CONTENT).asUTF8(), equalTo("<h1>A Nice Heading for jane</h1>"));
		log.debug("output: {}\n", data.get(Response.CONTENT));
		
		final AtomicInteger counter = new AtomicInteger();
		
		TestUtil.timed(10000, 100000, 4, new Runnable() {

			@Override
			public void run() {
				MutableContentStore data = MemoryContentStore.createMutableMap();
				data.put(path("name"), utf8("jane " + counter.incrementAndGet()));
				data.merge(jade.call(data));
			}
			
		});

	}
	
	 */
	
}
