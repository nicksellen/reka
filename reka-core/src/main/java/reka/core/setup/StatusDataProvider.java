package reka.core.setup;

import reka.api.data.MutableData;

public interface StatusDataProvider {
	boolean up();
	void statusData(MutableData data);
}