package reka.config.parser.handlers;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseHandler;
import reka.config.parser.values.BodyVal;
import reka.config.parser.values.DocVal;
import reka.config.parser.values.ValueVal;

public class ArrayItemHandler implements ParseHandler {

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
		
		ctx.parse(ParseHandlers.VALUE);
		
		Source src = ctx.source().subset(ctx.startPos(), ctx.endPos() - ctx.startPos());
		
		Config.ConfigBuilder conf = Config.newBuilder();
		
		conf.source(src);
		
		if (value != null) {
			conf.value(value.value());
		}
		
		if (body != null) {
			conf.body(ConfigBody.of(src, body.configs()));
		} else if (doc != null) {
			conf.document(doc.contentType(), doc.value());
		}
		
		/*
		String val = value != null ? value.value() : null;
		if (body != null) {
			ctx.emit("obj", obj(src, null, val, body.configs()));
		} else if (doc != null) {
			ctx.emit("doc", doc(src, null, val, doc.contentType(), doc.value()));
		} else if (value != null) {
			ctx.emit("kv", v(src, null, value.value()));
		} else {
			throw new RuntimeException("nothing!");
		}
		*/
		
		ctx.emit("config", conf.build());
		
		ctx.eat(ParseHandlers.WHITESPACE);
		
	}
	
}