package reka.api.data;

import static reka.api.content.Contents.integer;
import static reka.api.content.Contents.utf8;

import java.util.function.Consumer;

import reka.api.content.Content;

public interface ListMutation {
	
	ListMutation remove(int index);
	ListMutation add(Content content);
	ListMutation add(Data data);
	ListMutation addList(Consumer<ListMutation> list);
	ListMutation addMap(Consumer<MapMutation> map);
	
	default ListMutation addInt(int val) {
		return add(integer(val));
	}
	
	default ListMutation addString(String val) {
		return add(utf8(val));
	}
	
}
