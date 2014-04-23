package reka.configurer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Conf {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Deprecated {
		String value() default "";

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.METHOD)
		public @interface Warning {
			String value() default "";
		}
		
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Repeatable(MultiEachChildOf.class)
	public @interface EachChildOf {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MultiEachChildOf {
		EachChildOf[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Config {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Repeatable(MultiEach.class)
	public @interface Each {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MultiEach {
		Each[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface EachUnmatched {
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Repeatable(MultiAt.class)
	public @interface At {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MultiAt {
		At[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Val {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Key {
	}
	
}
