package reka.config.parser.handlers;

import static com.google.common.base.Preconditions.checkState;
import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseHandler;
import reka.config.parser.values.BodyVal;
import reka.config.parser.values.DocVal;
import reka.config.parser.values.KeyAndSubkey;
import reka.config.parser.values.ValueVal;

public class ItemHandler implements ParseHandler {

	private ValueVal value;
	private DocVal doc;
	private BodyVal body;
	
	public void receive(ValueVal value) {
		this.value = value;
	}
	
	public void receive(DocVal doc) {
		this.doc = doc;
	}

	public void receive(BodyVal body) {
		this.body = body;
	}
	
	@Override
	public void accept(ParseContext ctx) {
		
		KeyAndSubkey key = ctx.parseSync(ParseHandlers.KEY);
		
		checkState(!key.key().isEmpty(), "empty key at char %s in %s", ctx.startPos(), ctx.source().location());
		
		ctx.parse(ParseHandlers.VALUE);
		
		Source src = ctx.source().subset(ctx.startPos(), ctx.endPos() - ctx.startPos());
		
		Config.ConfigBuilder conf = Config.newBuilder();
		
		conf.key(key.key());
		conf.subkey(key.subkey());
		
		conf.source(src);
		
		if (value != null) {
			conf.value(value.value());
		}
		
		if (body != null) {
			conf.body(ConfigBody.of(src, body.configs()));
		} else if (doc != null) {
			conf.document(doc.contentType(), doc.value());
		}
		
		ctx.emit("config", conf.build());
		
		ctx.eat(ParseHandlers.WHITESPACE);
		
	}
	
}