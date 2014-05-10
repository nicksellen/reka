package reka.config.parser2;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.config.Source;

public final class ParseContext {
	
	private static class StackItem {
		
		final ParseState handler;
		
		int pos = -1;
		
		StackItem(ParseState handler) {
			this.handler = handler;
		}
		
	}
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	final Map<String,Object> toplevelEmissions = new HashMap<>();
	
	private final Deque<StackItem> stack = new ArrayDeque<>();
	
	private final StackItem root;
	
	private final char[] chars;
	private int nextPos = 0;
	
	public ParseContext(Source source, ParseState root) {
		this.root = new StackItem(root);
		chars = source.content().toCharArray();
	}
	
	public void emit(String name, Object emission) {
		emit(name, emission, -1, -1);
	}
	
	public void emit(String name, Object emission, int offset, int length) {
		
		if (stack.isEmpty()) {
			// top level
			if (toplevelEmissions.containsKey(name)) {
				log.warn("we alreayd had {}", name);
			}
			toplevelEmissions.put(name, emission);
			return;
		}
		
		Iterator<StackItem> it = stack.iterator();
		it.next(); // discard the top layer
		
		if (!it.hasNext()) {
			// top level
			if (toplevelEmissions.containsKey(name)) {
				log.warn("we alreayd had {}", name);
			}
			toplevelEmissions.put(name, emission);
			return;
		}
		
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
				StackItem topstate = stack.peek();
				
				int startPos, endPos;
				
				if (offset != -1 && length != -1) {
					startPos = topstate.pos + offset;
					endPos = startPos + length;
				} else {
					startPos = topstate.pos;
					endPos = nextPos;
				}
				
				log.info("emitting at {}..{} : {} [{}]", startPos, endPos, emission, new String(chars).substring(startPos, endPos));
			} else {
				log.error("{} should define a receive({} val) method then it would have gotton {}", 
						handlerClass.getName(), emission.getClass().getName(), emission);
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			log.error("{} should define a receive({} val) method", handlerClass.getName(), emission.getClass().getName());
		}
	}
	
	public void next(ParseState... next) {
		checkState(!stack.isEmpty(), "you can't call next with an empty stack");
		stack.pop();
		for (ParseState state : next) {
			push(new StackItem(state));
		}
	}

	public void start() {
		push(root);
	}
	
	public <T> T simpleParse(SimpleParseHandler<T> handler) {
		return handler.parse(this);
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
 
	public void take(ParseState state) {
		push(new StackItem(state));
	}
		
	private void push(StackItem state) {
		stack.push(state);
		state.pos = nextPos;
		state.handler.accept(this);
		if (!stack.isEmpty() && stack.peek().equals(state)) {
			stack.pop();
		}
		// TODO: the else happens if yuou call next() during it. I should make it so next() just pushes things onto the StackItem...
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
	
	@SuppressWarnings("unused")
	private String stackInfo() {
		return reverse(stack.stream().collect(toList())).stream().map(Object::getClass).map(Class::getSimpleName).collect(joining(" >> "));
	}

	public int eatUpTo(char c) {
		int len = 0;
		while (!isEOF() && popChar() != c) { /* yum yum */ len++; }
		return len;
	}
	
}