package reka.h2;

import static reka.util.Util.unchecked;

import org.h2.Driver;

import reka.core.bundle.UseConfigurer;
import reka.core.bundle.UseInit;

public class UseH2 extends UseConfigurer {

	@Override
	public void setup(UseInit init) {
		
		try {
			Class.forName(Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
		
	}

}
