package reka.net;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;
import io.netty.util.AttributeKey;

import java.util.Set;
import java.util.function.Predicate;

import reka.util.Identity;

public class ChannelAttrs {

	public static final AttributeKey<Identity> identity = AttributeKey.valueOf("identity");
	public static final AttributeKey<String> host = AttributeKey.valueOf("host");
	public static final AttributeKey<Integer> port = AttributeKey.valueOf("port");
	public static final AttributeKey<NetSettings.Type> type = AttributeKey.valueOf("type");
	public static final AttributeKey<String> id = AttributeKey.valueOf("id");
	public static final AttributeKey<Set<String>> tags = AttributeKey.valueOf("tags");
	
	public static class AttributeMatcher<T> implements ChannelMatcher, Predicate<Channel> {

		private final AttributeKey<T> key;
		private final T value;
		
		public AttributeMatcher(AttributeKey<T> key, T value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public boolean test(Channel channel) {
			return value != null && value.equals(channel.attr(key).get());
		}

		@Override
		public boolean matches(Channel channel) {
			return test(channel);
		}
		
	}
	
	public static class AttributeContainsMatcher<T> implements ChannelMatcher, Predicate<Channel> {

		private final AttributeKey<Set<T>> key;
		private final T value;
		
		public AttributeContainsMatcher(AttributeKey<Set<T>> key, T value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public boolean test(Channel channel) {
			Set<T> set = channel.attr(key).get();
			return set != null && set.contains(value);
		}

		@Override
		public boolean matches(Channel channel) {
			return test(channel);
		}
		
	}
	
}
