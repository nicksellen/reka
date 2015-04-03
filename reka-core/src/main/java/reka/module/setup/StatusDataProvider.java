package reka.module.setup;

import reka.data.MutableData;

public interface StatusDataProvider {
	boolean up();
	void statusData(MutableData data);
}