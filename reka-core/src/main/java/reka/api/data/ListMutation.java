package reka.api.data;

import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;
import reka.api.content.Content;
import reka.util.ThrowingConsumer;

public interface ListMutation {
	
	ListMutation remove(int index);
	ListMutation add(Content content);
	ListMutation add(Data data);
	ListMutation addList(ThrowingConsumer<ListMutation> list);
	ListMutation addMap(ThrowingConsumer<MapMutation> map);
	
	default ListMutation addInt(int val) {
		return add(integer(val));
	}
	
	default ListMutation addString(String val) {
		return add(utf8(val));
	}
	
}
