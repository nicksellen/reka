package reka.jdbc;

import static reka.core.builder.FlowSegments.storeSync;
import static reka.core.config.ConfigUtils.configToData;
import static reka.jdbc.JdbcModule.POOL;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.api.flow.FlowSegment;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;

public class JdbcInsertConfigurer implements Supplier<FlowSegment> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final List<Data> values = new ArrayList<>();
	private String table;

	@Conf.Val
	public void table(String val) {
		table = val;
	}
	
	@Conf.Each
	public void entry(Config config) {
		if (!config.hasBody()) return;
		values.add(configToData(config.body()));
	}
	
	@Override
	public FlowSegment get() {
		log.debug("building jdbc insert with values [{}]", values);
		return storeSync("insert", store -> new JdbcInsert(store.get(POOL), table, values));
	}

}
