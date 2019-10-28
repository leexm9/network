package com.leexm.demo.network.netty;

import com.leexm.demo.network.netty.handler.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author leexm
 * @date 2019-10-28 23:11
 */
public class NettyClient {

    private static final int PORT = 8088;

    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(HOST, PORT)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ClientHandler());
                    }
                });

        try {
            for (int i = 0; i < 10; i++) {
                // Start the client.
                ChannelFuture future = bootstrap.connect().sync();
                // Wait until the connection is closed.
                future.channel().closeFuture().sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

}
