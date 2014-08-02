package reka.gitfordata.tree.record.builders;

import reka.api.content.Content;
import reka.gitfordata.tree.RecordBuilder;
import reka.gitfordata.tree.record.ContentRecord;
import reka.gitfordata.tree.record.DefaultContentRecord;

public class ContentRecordBuilder implements RecordBuilder<ContentRecord> {
	
	private final Content content;
	
	public ContentRecordBuilder(Content content) {
		this.content = content;
	}

	@Override
	public ContentRecord build(RecordCollector collector) {
		return DefaultContentRecord.fromContent(content);
	}


}
