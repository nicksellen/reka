package reka.gitfordata.tree.record;

import reka.api.content.Content;
import reka.gitfordata.tree.Record;

public interface ContentRecord extends Record {
	
	public Content.Type contentType();
	public Content toContent();
	
	default int size() {
		return 0;
	}
	
}