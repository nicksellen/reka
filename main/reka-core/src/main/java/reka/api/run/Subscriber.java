package reka.api.run;

import reka.api.data.MutableData;

public interface Subscriber {
	void ok(MutableData data);
}
