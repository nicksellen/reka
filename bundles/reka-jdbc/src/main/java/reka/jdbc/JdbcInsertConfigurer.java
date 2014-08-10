package reka.jdbc;

import static reka.core.builder.FlowSegments.sync;
import static reka.core.config.ConfigUtils.configToData;

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
	private final JdbcConnectionProvider jdbc;
	private String table;
	
	public JdbcInsertConfigurer(JdbcConnectionProvider jdbc) {
		this.jdbc = jdbc;
	}

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
		return sync("insert", () -> new JdbcInsert(jdbc, table, values));
	}

}
