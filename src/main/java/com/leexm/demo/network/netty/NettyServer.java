package com.leexm.demo.network.netty;

import com.leexm.demo.network.netty.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Netty 实现
 *
 * @author leexm
 * @date 2019-10-28 23:11
 */
public class NettyServer {

    private static final int PORT = 8088;

    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup workers = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .handler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel ch) throws Exception {
                        System.out.println("进入boss 线程的 handler");
                    }
                })
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast("server-handler", new ServerHandler());
                    }
                });

        serverBootstrap.bind(PORT).addListener(future -> {
            System.out.println("服务器启动，等待连接中......");
        });
    }

}
