package reka.api.data;

import java.util.AbstractList;

public class DataListView extends AbstractList<Object> {

	private final Data data;
	
	public DataListView(Data data) {
		this.data = data;
	}

	@Override
	public Object get(int index) {
		return DataViewUtil.convert(data.at(index));
	}

	@Override
	public int size() {
		return data.size();
	}

}
