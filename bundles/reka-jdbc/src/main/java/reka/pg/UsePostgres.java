package reka.pg;

import static reka.util.Util.unchecked;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UsePostgres extends UseConfigurer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public void setup(UseInit init) {
		try {
			Class.forName(Driver.class.getName());
			log.info("loading postgres driver {}", Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
	}

}
