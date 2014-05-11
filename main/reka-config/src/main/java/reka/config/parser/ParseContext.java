package reka.config.parser;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.reverse;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Source;

public final class ParseContext {

	private static final Logger log = LoggerFactory.getLogger(ParseContext.class);
	
	private static class StackItem {
		
		final ParseState handler;
		
		int pos = -1;

		private StackItem next = null;
		
		StackItem(ParseState handler) {
			this.handler = handler;
		}
		
		public String name() {
			return format("%s(%s)", handler.getClass().getSimpleName().replaceFirst("State$", ""), System.identityHashCode(handler));
		}
		
		@Override
		public String toString() {
			return name();
		}

		public void next(StackItem val) {
			if (val == null) {
				next = null;
			} else {
				checkState(next == null, "can only set one next value");
				next = val;
			}
		}
		
	}
	
	private final Deque<StackItem> stack = new ArrayDeque<StackItem>() {

		private static final long serialVersionUID = 8574738932762696054L;
		
		@Override
		public String toString() {
			return reverse(stack.stream().collect(toList())).stream()
					.map(StackItem::name).collect(joining(" >> "));
		}
		
	};
	
	private final StackItem root;
	private final Source source;
	
	private final char[] chars;
	private int nextPos = 0;
	
	public ParseContext(Source source, ParseState root) {
		this.source = source;
		this.root = new StackItem(root);
		chars = source.content().toCharArray();
	}
	
	public Source source() {
		return source;
	}
	
	public void emit(String name, Object emission) {
		emit(name, emission, 0, -1);
	}
	
	public void emit(String name, Object emission, int offset, int length) {
		
		if (stack.isEmpty()) return;
		
		Iterator<StackItem> it = stack.iterator();
		it.next(); // ignore the top layer (this is the one doing the emitting)
		
		if (!it.hasNext()) return;
		
		StackItem state = it.next();
		
		Method method = null;
		Class<?> objClass = emission.getClass();
		Class<?> handlerClass = state.handler.getClass();
		
		try {
			for (Method m : handlerClass.getMethods()) {
				if (!"receive".equals(m.getName())) continue;
				if (m.getParameterCount() != 1) continue;
				Class<?> klass = m.getParameterTypes()[0];
				if (!klass.isAssignableFrom(objClass)) continue;
				method = m;
				break;
			}
			if (method != null) {
				method.invoke(state.handler, emission);
			} else {
				log.error("{} should define a receive({} val) method then it would have gotton {}", 
						handlerClass.getName(), emission.getClass().getName(), emission);
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("{} should define a receive({} val) method", handlerClass.getName(), emission.getClass().getName());
		}
	}
	
	public void next(ParseState next) {
		checkState(!stack.isEmpty(), "you can't call next with an empty stack");
		StackItem n = new StackItem(next);
		stack.peek().next(n);
	}

	public void run() {
		process(root);
	}
	
	public <V> V simpleParse(SimpleParseHandler<V> handler) {
		return handler.apply(this);
	}
	
	public void eat(EatHandler handler) {
		handler.eat(this);
	}
	
	public void eatCharIf(CharPredicate predicate) {
		eat(ctx -> {
			while (!ctx.isEOF() && predicate.test(ctx.peekChar())) {
				ctx.popChar();
			}
		});
	}
	
	public <T> T provide(Supplier<T> supplier) {
		return supplier.get();
	}
 
	public void parse(ParseState state) {
		process(new StackItem(state));
	}
		
	private void process(StackItem state) {
		
		stack.push(state);
		
		state.pos = nextPos;
		state.handler.accept(this);
		
		StackItem top = stack.pop();

		checkState(top.equals(state), "hmpph!");
		if (state.next != null) {
			StackItem next = state.next;
			state.next(null);
			process(next);
		}
	}
	
	public boolean isEOF() {
		return nextPos >= chars.length;
	}
	
	public char popChar() {
		checkState(!isEOF(), "reached end!");
		return chars[nextPos++];
	}
	
	public char[] popChars(int len) {
		char[] a = new char[len];
		for (int i = 0; i < len; i++) {
			a[i] = popChar();
		}
		return a;
	}
	
	public char peekChar() {
		return chars[nextPos];
	}

	public int eatUpTo(char c) {
		int len = 0;
		while (!isEOF() && popChar() != c) { /* yum yum */ len++; }
		return len;
	}

	public void printStack() {
		log.info("stack: {}", stack);
	}

	public int startPos() {
		return stack.peek().pos;
	}
	
	public int endPos() {
		return nextPos;
	}
	
}