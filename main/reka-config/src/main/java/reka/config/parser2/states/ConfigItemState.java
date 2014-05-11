package reka.config.parser2.states;

import static com.google.common.base.Preconditions.checkState;
import static reka.config.ConfigUtil.doc;
import static reka.config.ConfigUtil.k;
import static reka.config.ConfigUtil.kv;
import static reka.config.ConfigUtil.obj;
import reka.config.Source;
import reka.config.parser2.ParseContext;
import reka.config.parser2.ParseState;
import reka.config.parser2.Parser2.BodyVal;
import reka.config.parser2.Parser2.DocVal;
import reka.config.parser2.Parser2.KeyVal;
import reka.config.parser2.Parser2.ValueVal;

public class ConfigItemState implements ParseState {

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
		
		KeyVal key = ctx.simpleParse(State.KEY);
		
		checkState(!key.value().isEmpty(), "empty key at char %s in %s", ctx.startPos(), ctx.source().location());
		
		ctx.parse(State.VALUE);
		
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
		
		ctx.eat(State.WHITESPACE);
		
	}
	
}