package reka;

import static reka.config.configurer.Configurer.configure;
import static reka.config.configurer.Configurer.Preconditions.checkConfig;
import static reka.core.config.ConfigUtils.configToData;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.codehaus.jackson.map.ObjectMapper;

import reka.FlowTest.FlowTestCase;
import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;
import reka.core.data.memory.MutableMemoryData;

public class TestConfigurer {
	
	private static final ObjectMapper jsonMapper = new ObjectMapper();

	private final ConfigurerProvider provider;
	
	private final List<FlowTestCase> cases = new ArrayList<>();
	
	private Supplier<FlowSegment> run;
	
	private Data initial = Data.NONE;
	private Data expect = Data.NONE;
	
	public TestConfigurer(ConfigurerProvider provider) {
		this.provider = provider;
	}

	@Conf.At("data")
	public void initial(Config config) {
		initial = configToData(config.body());
	}
	
	@Conf.At("run")
	public void run(Config config) {
		if (config.hasBody()) {
			run = configure(new SequenceConfigurer(provider), config.body()).bind();
		} else if (config.hasValue()) {
			run = provider.provide("run", provider, config);
		}
	}
	
	@Conf.At("expect")
	public void expect(Config config) {
		if (config.hasBody()) {
			expect = configToData(config.body());
		} else if (config.hasDocument()) {
			expect = jsonStringToData(config.documentContentAsString());
		}
	}
	
	@Conf.Each("case")
	public void testCase(Config config) {
		String name = config.valueAsString();
		configure(new TestCaseConfigurer(name), config.body()).addToList(cases);;
	}
	
	public static class TestCaseConfigurer {
		
		private final String name;

		private Data initial = Data.NONE;
		private Data expect = Data.NONE;
		
		public TestCaseConfigurer(String name) {
			this.name = name;
		}
		
		@Conf.At("input")
		public void input(Config config) {
			initial = configToData(config.body());
		}
		
		@Conf.At("expect")
		public void expect(Config config) {
			if (config.hasBody()) {
				expect = configToData(config.body());
			} else if (config.hasDocument()) {
				expect = jsonStringToData(config.documentContentAsString());
			}
		}
		
		public void addToList(List<FlowTestCase> testCases) {
			if (!initial.equals(Data.NONE) && !expect.equals(Data.NONE)) {
				testCases.add(new FlowTestCase(name, initial, expect));	
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private static Data jsonStringToData(String jsonString) {
		try {
			return MutableMemoryData.createFromMap(jsonMapper.readValue(jsonString, Map.class));
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public FlowTest build() {
		List<FlowTestCase> testCases = new ArrayList<>(cases);
		if (!expect.equals(Data.NONE)) {
			testCases.add(new FlowTestCase("main", initial, expect));
		}
		return new FlowTest(run, testCases);
	}
	
}
