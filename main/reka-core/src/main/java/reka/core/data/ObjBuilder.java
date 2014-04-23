package reka.core.data;

public interface ObjBuilder {
	void writeStartMap();
	void writeEndMap();
	void writeStartList();
	void writeEndList();
	void writeFieldName(String name);
	void writeValue(Object value);
	Object obj();

}