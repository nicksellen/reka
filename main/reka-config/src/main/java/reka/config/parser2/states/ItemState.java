package reka.config.parser2.states;

import static java.lang.Character.isWhitespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.parser2.ParseContext;
import reka.config.parser2.ParseState;
import reka.config.parser2.Parser.BodyVal;
import reka.config.parser2.Parser.DocVal;
import reka.config.parser2.Parser.KeyAndValueItem;
import reka.config.parser2.Parser.KeyOnlyItem;
import reka.config.parser2.Parser.KeyVal;
import reka.config.parser2.Parser.ValueVal;

public class ItemState implements ParseState {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

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
		
		KeyVal key = ctx.simpleParse(Stateless.KEY2);
		
		ctx.take(Stateless.VALUE);
		
		if (doc != null) {
			log.info("we got a doc! {}", doc);
		}
		
		if (body != null) {
			log.info("we got a body! {}", body);
		}
		
		if (key != null && value != null) {
			log.info("emitting key and val {}:{} (doc={})", key, value, doc);
			ctx.emit("key and value item", new KeyAndValueItem(key, value));
		} else if (key != null) {
			log.info("emitting key {}", key);
			ctx.emit("key item", new KeyOnlyItem(key));
		}
		
		ctx.eatCharIf(c -> isWhitespace(c));
		
	}
	
}