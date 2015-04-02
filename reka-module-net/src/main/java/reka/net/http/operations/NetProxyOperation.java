package reka.net.http.operations;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import reka.api.data.MutableData;
import reka.api.run.AsyncOperation;
import reka.api.run.OperationContext;
import reka.net.NetModule;

public class NetProxyOperation implements AsyncOperation {
	
	private final EventLoopGroup group;
	
	private final int port;
	private final String host;

	public NetProxyOperation(EventLoopGroup group, String host, int port) {
		this.group = group;
		this.host = host;
		this.port = port;
	}
	
	public static class ProxyInbound extends ChannelInboundHandlerAdapter {
		
		private final Channel outboundChannel;
		
		public ProxyInbound(Channel outboundChannel) {
			this.outboundChannel = outboundChannel;
		}

	    @Override
	    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
	        if (outboundChannel.isActive()) {
	            outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
	                @Override
	                public void operationComplete(ChannelFuture future) {
	                    if (future.isSuccess()) {
	                        ctx.channel().read();
	                    } else {
	                        future.channel().close();
	                    }
	                }
	            });
	        }
	    }

	    @Override
	    public void channelInactive(ChannelHandlerContext ctx) {
	        if (outboundChannel != null) {
	            closeOnFlush(outboundChannel);
	        }
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	        cause.printStackTrace();
	        closeOnFlush(ctx.channel());
	    }
	    
	    static void closeOnFlush(Channel ch) {
	        if (ch.isActive()) {
	            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	        }
	    }
	}
	
	public static class ProxyOutbound extends ChannelInboundHandlerAdapter {

		private final Channel inboundChannel;

		public ProxyOutbound(Channel inboundChannel) {
			this.inboundChannel = inboundChannel;
		}

		@Override
		public void channelActive(ChannelHandlerContext ctx) {
			ctx.read();
			ctx.write(Unpooled.EMPTY_BUFFER);
		}

		@Override
		public void channelRead(final ChannelHandlerContext ctx, Object msg) {
			inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						ctx.channel().read();
					} else {
						future.channel().close();
					}
				}
			});
		}
		
	}

	@Override
	public void call(MutableData data, OperationContext ctx, OperationResult res) {
		
		Channel inboundChannel = ctx.get(NetModule.Keys.channel);
		
		clearChannelPipeline(inboundChannel.pipeline());
		
		Bootstrap bootstrap = new Bootstrap()
			.group(group)
			.channel(inboundChannel.getClass())
			.handler(new ProxyOutbound(inboundChannel))
			.option(ChannelOption.AUTO_READ, false);
		
		ChannelFuture f = bootstrap.connect(host, port);
		Channel outboundChannel = f.channel();
		inboundChannel.pipeline().addLast(new ProxyInbound(outboundChannel));
		
		f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    inboundChannel.read();
                } else {
                    inboundChannel.close();
                }
            }
        });
	}

	private void clearChannelPipeline(ChannelPipeline pipeline) {
		@SuppressWarnings("unused")
		ChannelHandler handler;
		while ((handler = pipeline.last()) != null) {
			pipeline.removeLast();
		}
	}
	
}
