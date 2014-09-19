package reka;

import static reka.config.configurer.Configurer.configure;
import static reka.core.config.ConfigUtils.configToData;

import java.util.function.Supplier;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.config.ConfigurerProvider;
import reka.core.config.SequenceConfigurer;

public class TestConfigurer {

	private final ConfigurerProvider provider;
	
	private Data initial = Data.NONE;
	private Supplier<FlowSegment> run;
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
		expect = configToData(config.body());
	}
	
	public FlowTest build() {
		return new FlowTest(initial, run, expect);
	}
	
}
