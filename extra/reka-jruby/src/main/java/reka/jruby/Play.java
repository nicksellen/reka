package reka.jruby;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyNil;
import org.jruby.embed.ScriptingContainer;
import org.jruby.java.proxies.MapJavaProxy;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Play {
	
	private static final Logger log = LoggerFactory.getLogger(Play.class);
	
	// https://github.com/jruby/jruby/wiki/RedBridge
	// https://github.com/jruby/jruby/wiki/DirectJRubyEmbedding
	
	public static void main(String[] args) {
		ScriptingContainer container = new ScriptingContainer();
		
		container.runScriptlet("class Nick; def foo; puts 'foo'; end; end; def iamfree(v); puts 'i am! ' + v; end");

		container.put("name", "nick");
		
		container.runScriptlet("Nick.new.foo; iamfree(name)");
	}
	
    public static void main2(String[] args) {
        Ruby ruby = Ruby.newInstance();
        IRubyObject r = ruby.evalScriptlet("class Nick; def foo; puts 'foo'; end; end; def iamfree(v); puts 'i am! ' + v.class; end");
        
        log.debug("type is [{}]\n", r.getType());
        
        
        for (String name : r.getVariableNameList()) {
        	log.debug("var: {}\n", name);
        }
        
        ThreadContext context = ruby.getCurrentContext();
        
        StaticScope ss = ruby.getStaticScopeFactory().newEvalScope(context.getCurrentStaticScope());
        
        ss.addVariable("name");
        
        log.debug("{}ss has {} variables\n", "", asList(ss.getVariables()));
        
        
        ManyVarsDynamicScope manyVarsScope = new ManyVarsDynamicScope(ss, context.getCurrentScope());
        
        
        DynamicScope scope;
        
        //scope = manyVarsScope.getEvalScope(ruby);
        scope = manyVarsScope;
        
        log.debug("{}names in scope {}\n", "", asList(scope.getAllNamesInScope()));
        
        //DynamicScope scope = DynamicScope.newDynamicScope(context.getCurrentStaticScope(), context.getCurrentScope());
        
        
        
        //Node code = ruby.parseEval(content, file, scope, lineNumber)
        
        //RubyClass.newClass(ruby, RubyClass.newm)
        Map<String,Object> data = new HashMap<>();

//new RubyCon
        JavaEmbedUtils.javaToRuby(ruby, data);
        
        //RubyObjectAdapter adapter = JavaEmbedUtils.newObjectAdapter();
        
        RubyHash h = new RubyHash(ruby, data, new RubyNil(ruby));
        h.put("name", "nick");
        
        MapJavaProxy proxy = new MapJavaProxy(ruby, ruby.getObject(), data);
        
        //VariableInterceptor.inject(map, runtime, scope, depth, receiver);
        scope.setValueZeroDepthZero(proxy);
        
        //scope.setValueDepthZero(proxy, 0);
        
        ruby.evalScriptlet("Nick.new.foo; iamfree(name)", scope);
    }
}
