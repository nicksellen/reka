package reka.jdbc;

import static reka.core.config.ConfigUtils.configToData;
import static reka.jdbc.JdbcModule.POOL;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.Data;
import reka.config.Config;
import reka.config.configurer.annotations.Conf;
import reka.core.bundle.OperationSetup;
import reka.nashorn.OperationsConfigurer;

public class JdbcInsertConfigurer implements OperationsConfigurer {
	
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
		ops.add("insert", store -> new JdbcInsert(store.get(POOL), table, values));
	}

}
