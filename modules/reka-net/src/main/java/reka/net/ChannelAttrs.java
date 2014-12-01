package reka.net;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelMatcher;
import io.netty.util.AttributeKey;

import java.util.Set;
import java.util.function.Predicate;

public class ChannelAttrs {
	
	public static final AttributeKey<String> host = AttributeKey.valueOf("host");
	public static final AttributeKey<String> id = AttributeKey.valueOf("id");
	public static final AttributeKey<Set<String>> topics = AttributeKey.valueOf("topics");
	
	public static class ChannelTopicMatcher implements ChannelMatcher, Predicate<Channel>  {

		private final String topic;
		
		public ChannelTopicMatcher(String topic) {
			this.topic = topic;
		}

		@Override
		public boolean matches(Channel channel) {
			return channel.attr(ChannelAttrs.topics).get().contains(topic);
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
	
}
