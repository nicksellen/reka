package reka.config.parser;

import static reka.config.ConfigUtil.doc;
import static reka.config.ConfigUtil.kv;
import static reka.config.ConfigUtil.obj;

import java.util.ArrayList;
import java.util.List;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Config;
import reka.config.ConfigBody;
import reka.config.Source;
import reka.config.SubsetSource;

import com.google.common.collect.ImmutableList;

class ParsedItem implements Locatable, ParsedItemProvider {
	
	private static final Logger log = LoggerFactory.getLogger(ParsedItem.class);

    static final Parser PARSER = Parboiled.createParser(Parser.class);
    
	public static ParsedItem from(Source source) {
		ParseRunner<?> runner = new BasicParseRunner<>(PARSER.Main(source));
		ParsingResult<?> result = runner.run(source.content());
		if (result.hasErrors()) {
			log.debug("\nParse Errors:\n" + ErrorUtils.printParseErrors(result));
		}
		return (ParsedItem) result.resultValue;
	}
	
	protected final boolean comment;
	protected final String key;
	protected final List<ParsedItem> children = new ArrayList<>();
	
	private final boolean isRoot;
	
	private boolean isObject = false;
	
	protected Source source;
	
	private ParsedValue value;
	private ParsedDocument document;
	private ParsedData data;
	
	private ParsedSourceLocation pos;
	
	protected static ParsedItem root(Source source) {
		return new ParsedItem(null, false, true, source);
	}
	
	protected static ParsedItem create(String key, Source source) {
		return new ParsedItem(key, false, false, source);
	}

	protected static ParsedItem comment(String key, Source source) {
		return new ParsedItem(key, true, false, source);
	}
	
	private ParsedItem(String key, boolean comment, boolean root, Source source) {
		this.key = key;
		this.comment = comment;
		this.isRoot = root;
		this.source = source;
	}
	
	public ConfigBody toConfig() {
	    return build(this);
	}
    
    private static ConfigBody build(ParsedItem item) {
        ConfigBody result;
        Object value = item.value() != null ? item.value().val() : null;
        
        Source source = new SubsetSource(item.source, item.pos.start(), item.pos.length()); 
        
        if (item.isObject()) {
            List<Config> children = new ArrayList<>();
            for (ParsedItem child : item.children) {
                for (Config c : build(child)) {
                    children.add(c);
                }
            }
            if (item.isRoot) {
                result = ConfigBody.of(source, children);
            } else {
                result = ConfigBody.of(source, obj(source, item.key, value, ImmutableList.<Config>copyOf(children)));
            }
        } else {
            if (item.heredoc() != null) {
                result = ConfigBody.of(source, doc(source, item.key, value, item.heredoc().type(), item.heredoc().content()));
            } else {
                result = ConfigBody.of(source, kv(source, item.key, value));
            }
        }
        
        return result;
    }
	
	public boolean isObject() {
		return isObject;
	}
	
	public List<ParsedItem> children() {
		return children;
	}
	
	public String key() {
		return key;
	}
	
	public ParsedValue value() {
		return value;
	}
	
	protected void value(ParsedValue value) {
		this.value = value;
	}
	
	public boolean comment() {
	    return comment;
	}
	
	@Override
	public void sourceLocation(ParsedSourceLocation pos) {
		this.pos = pos;
	}
	
	@Override
	public ParsedSourceLocation sourceLocation() {
		return pos;
	}
	
	public ParsedDocument heredoc() {
		return document;
	}
	
	protected void document(ParsedDocument heredoc) {
		this.document = heredoc;
	}
	
	public ParsedData data() {
		return data;
	}
	
	protected void markAsObject() {
		isObject = true;
	}
	
	private void source(Source source) {
		this.source = source;
	}
	
	public Source source() {
	    return source;
	}
	
	protected void addChild(ParsedItem child) {
		child.source(source);
		children.add(child);
	}

    @Override
    public ParsedItem buildConfigItem() {
        return this;
    }
}