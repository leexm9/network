package com.leexm.demo.network.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

/**
 * @author leexm
 * @date 2019-10-28 23:29
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        String message = byteBuf.toString(Charset.forName("UTF-8"));
        System.out.println(String.format("线程%s，收到客户端信息:%s", Thread.currentThread().getName(), message));

        ByteBuf out = getByteBuf(ctx, String.format("[%s]", message));
        ctx.channel().writeAndFlush(out);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    private ByteBuf getByteBuf(ChannelHandlerContext ctx, String msg) {
        ByteBuf byteBuf = ctx.alloc().buffer();
        byte[] bytes = msg.getBytes(Charset.forName("UTF-8"));
        byteBuf.writeBytes(bytes);
        return byteBuf;
    }

}
