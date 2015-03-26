package reka.net;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;
import io.netty.util.AttributeKey;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

import reka.Identity;

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
			return value.equals(channel.attr(key).get());
		}

		@Override
		public boolean matches(Channel channel) {
			return matches(channel);
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
			return channel.attr(key).get().contains(value);
		}

		@Override
		public boolean matches(Channel channel) {
			return matches(channel);
		}
		
	}
	
	/*
	public static class ChannelTagMatcher implements ChannelMatcher, Predicate<Channel>  {

		private final String tag;
		
		public ChannelTagMatcher(String tag) {
			this.tag = tag;
		}

		@Override
		public boolean matches(Channel channel) {
			return channel.attr(ChannelAttrs.tags).get().contains(tag);
		}
		
		@Override
		public boolean test(Channel channel) {
			return matches(channel);
		}
		
	}
	
	public static class ChannelHostMatcher implements ChannelMatcher, Predicate<Channel>  {

		private final String host;
		
		public ChannelHostMatcher(String host) {
			this.host = host;
		}
		
		@Override
		public boolean matches(Channel channel) {
			return host.equals(channel.attr(ChannelAttrs.host).get());
		}

		@Override
		public boolean test(Channel channel) {
			return matches(channel);
		}
		
	}
	
	public static class ChannelIdentityMatcher implements ChannelMatcher, Predicate<Channel> {

		private final Identity identity;
		
		public ChannelIdentityMatcher(Identity identity) {
			this.identity = identity;
		}
		
		@Override
		public boolean matches(Channel channel) {
			return id.equals(channel.attr(ChannelAttrs.id).get());
		}

		@Override
		public boolean test(Channel channel) {
			return matches(channel);
		}
		
	}
	
	public static class ChannelIdMatcher implements ChannelMatcher, Predicate<Channel> {

		private final String id;
		
		public ChannelIdMatcher(String id) {
			this.id = id;
		}
		
		@Override
		public boolean matches(Channel channel) {
			return id.equals(channel.attr(ChannelAttrs.id).get());
		}

		@Override
		public boolean test(Channel channel) {
			return matches(channel);
		}
		
	}
	*/
	
}
