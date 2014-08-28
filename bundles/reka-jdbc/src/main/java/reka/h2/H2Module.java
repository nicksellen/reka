package reka.h2;

import static reka.util.Util.unchecked;

import org.h2.Driver;

import reka.core.bundle.ModuleConfigurer;
import reka.core.bundle.ModuleSetup;

public class H2Module extends ModuleConfigurer {

	@Override
	public void setup(ModuleSetup module) {
		
		try {
			Class.forName(Driver.class.getName());
		} catch (ClassNotFoundException e) {
			throw unchecked(e);
		}
		
	}

}
