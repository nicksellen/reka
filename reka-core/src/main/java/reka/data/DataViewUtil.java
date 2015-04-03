package reka.data;

public class DataViewUtil {

	public static Object convert(Data data) {
		if (data == null) return null;
		if (!data.isPresent()) return null;
		if (data.isMap()) {
			return new DataMapView(data);
		} else if (data.isList()) {
			return new DataListView(data);
		} else if (data.isContent()) {
			return data.content().value();
		} else {
			return null;
		}
	}
	
}
