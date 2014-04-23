package reka.config.parser;

import static reka.config.formatters.FormattingUtil.removeIndentation;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.support.Position;

import reka.config.Source;

// https://github.com/sirthias/parboiled/wiki/Rule-Construction-in-Java

class Parser extends BaseParser<Object> {

	final Rule NEWLINE = Sequence(Optional('\r'), '\n');
	final Rule SpaceChar = AnyOf(" \t");
	final Rule Space = OneOrMore(SpaceChar);
	final Rule OptionalSpace = Optional(Space);
	final Rule LeftBrace = Ch('{');
	final Rule RightBrace = Ch('}');
	final Rule WSCh = AnyOf(" \n\r\t\f");
	final Rule WS = ZeroOrMore(WSCh);
	final Rule Digit = CharRange('0', '9');
	final Rule Letter = FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'));
	final Rule Alphanumeric = FirstOf(Letter, Digit);
	final Rule NormalChar = Sequence(TestNot(AnyOf("\n")), ANY);
	
	//final Rule SymbolChar = AnyOf(":/\\.-_!?><()[]'\"=,*&^%$£@|:;~#+`");
	//final Rule SymbolChar = Sequence(TestNot(AnyOf(" \n\r\t\f{}")), ANY);
	final Rule SymbolChar = Sequence(TestNot(AnyOf(" \n\r\t\f{}")), ANY);
	
	final Rule BlockStart = Sequence(LeftBrace, OptionalSpace, NEWLINE);
	final Rule BlockEnd = RightBrace;
	final Rule TextLine = OneOrMore(NormalChar);
	final Rule DocumentStart = String("<<-");
	final Rule DocumentEnd = Sequence(NEWLINE, OptionalSpace, String("---"),
			ZeroOrMore('-'), OptionalSpace, FirstOf(NEWLINE, EOI));
	
	final Rule Identifier = OneOrMore(Sequence(
			TestNot(WSCh, DocumentStart, BlockStart),
			FirstOf(Alphanumeric, SymbolChar)));

	Rule DocumentText(Rule end) {
		return OneOrMore(Except(FirstOf(NormalChar, WSCh, AnyOf("{}")), end));
	}

	Rule Main(Source source) {
		return Sequence(push(ParsedItem.root(source)), Body(), addSrcPos());
	}
/*
	Rule Identifier() {
		return OneOrMore(FirstOf(Alphanumeric, SymbolChar));
	}
	*/

	String popString(int num) {
		return (String) pop(num);
	}

	Object popDocumentBody() {
		return pop();
	}

	String popString() {
		return (String) pop();
	}

	ParsedValue popValue() {
		return (ParsedValue) pop();
	}

	ParsedItem popConfigObject() {
		return (ParsedItem) pop();
	}

	Rule StringValue() {
		return Sequence(
				push(position()),
				OneOrMore(Except(
						NormalChar,
						//FirstOf(Alphanumeric, SpaceChar,
								//AnyOf(":/\\.-_!?>()[]{}'\"=,*&^%$£@|:;~#+`") // like
																				// NormalChar()
																				// but
																				// no
																				// <
																				// and
																				// added
																				// {}
						//),
						FirstOf( 
							DocumentStart,
							BlockStart
						))),
						push(new ParsedString(popPosition(), match().trim())), 
						addSrcPos());
	}

	Rule PropertyKey() {
		return Sequence(Identifier, push(match().trim()));
	}

	boolean addValue(ParsedValue value) {
		((ParsedItem) peek()).value(value);
		return true;
	}

	boolean createDocument() {
		String key = popString(1);
		Object body = pop();
		ParsedDocument doc = ParsedStringDocument.create(key, body.toString());
		((ParsedItem) peek()).document(doc);
		return true;
	}

	boolean markAsObject() {
		((ParsedItem) peek()).markAsObject();
		return true;
	}

	boolean addToParent(ParsedItem child) {
		((ParsedItem) peek()).addChild(child);
		return true;
	}

	Rule BlockValue() {
		return Sequence(BlockStart, Body(), BlockEnd);
	}

	boolean addSrcPos() {
		((Locatable) peek()).sourceLocation(srcPos());
		return true;
	}

	Rule Body() {
		return Sequence(markAsObject(), WS,
				ZeroOrMore(Sequence(Item(), addToParent(popConfigObject()))));
	}

	Position popPosition() {
		return (Position) pop();
	}

	Position popPosition(int num) {
		return (Position) pop(num);
	}

	Rule Item() {
		return Sequence(ItemReal(), addSrcPos(), WS);
	}

	Source configSource() {
		return ((ParsedItem) peek()).source;
	}

	Rule ItemReal() {
		return FirstOf(
			Sequence(LineComment(), push(ParsedItem.comment(popString(), configSource()))),
			Sequence(PropertyKey(), push(ParsedItem.create(popString(), configSource())),
				Optional(Sequence(
					Space,
					FirstOf(
							StringValue(),
							push(null)),
					addValue(popValue()),
					Optional(
						FirstOf(
							Sequence(
								DocumentValue(),
						 		createDocument(), addSrcPos()),
							BlockValue()
						)
					)
				))
			)
		);
	}

	Rule Except(Rule rule, Rule except) {
		return Sequence(TestNot(except), rule);
	}
	
	Rule DocumentValue() {
		return Sequence(
				DocumentStart,
				ZeroOrMore('-'),
				OptionalSpace,
				FirstOf(Sequence(Identifier, push(match().trim())), push(null)),
				Optional(Sequence(Space, OneOrMore('-'))),
				OptionalSpace,
				NEWLINE,
				Sequence(DocumentText(DocumentEnd), push(removeIndentation(match()))), DocumentEnd);
	}

	ParsedSourceLocation srcPos() {
		return ParsedSourceLocation.create(matchStart(), matchLength());
	}
	
	Rule LineComment() {
		return Sequence('#', Sequence(TextLine, push(match().trim())), WS);
	}

}
