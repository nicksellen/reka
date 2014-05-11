package reka.config.parser.states;

import static com.google.common.base.Preconditions.checkState;
import static reka.config.ConfigUtil.doc;
import static reka.config.ConfigUtil.k;
import static reka.config.ConfigUtil.kv;
import static reka.config.ConfigUtil.obj;
import reka.config.Source;
import reka.config.parser.ParseContext;
import reka.config.parser.ParseState;
import reka.config.parser.values.BodyVal;
import reka.config.parser.values.DocVal;
import reka.config.parser.values.KeyVal;
import reka.config.parser.values.ValueVal;

public class ItemState implements ParseState {

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
		
		KeyVal key = ctx.simpleParse(ParseStates.KEY);
		
		checkState(!key.value().isEmpty(), "empty key at char %s in %s", ctx.startPos(), ctx.source().location());
		
		ctx.parse(ParseStates.VALUE);
		
		Source src = ctx.source().subset(ctx.startPos(), ctx.endPos() - ctx.startPos());
		
		String val = value != null ? value.value() : null;
		if (body != null) {
			ctx.emit("obj", obj(src, key.value(), val, body.configs()));
		} else if (doc != null) {
			ctx.emit("doc", doc(src, key.value(), val, doc.contentType(), doc.value()));
		} else if (key != null && value != null) {
			ctx.emit("kv", kv(src, key.value(), value.value()));
		} else if (key != null) {
			ctx.emit("k", k(src, key.value()));
		}
		
		ctx.eat(ParseStates.WHITESPACE);
		
	}
	
}