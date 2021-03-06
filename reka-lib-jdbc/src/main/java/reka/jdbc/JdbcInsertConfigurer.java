package reka.jdbc;

import static reka.core.config.ConfigUtils.configToData;
import static reka.jdbc.JdbcBaseModule.POOL;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.data.Data;
import reka.module.setup.OperationConfigurer;
import reka.module.setup.OperationSetup;

public class JdbcInsertConfigurer implements OperationConfigurer {
	
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
	public void setup(OperationSetup ops) {
		log.debug("building jdbc insert with values [{}]", values);
		ops.add("insert", () -> new JdbcInsert(ops.ctx().get(POOL), table, values));
	}

}
