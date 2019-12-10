package com.leexm.demo.network.nio.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 服务端可以接受客户端重复的请求
 *
 * @author lxm
 * @date 2019/10/25 13:09
 */
public class NioServer2 {

    private static final int PORT = 8088;

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("服务器启动，等待客户端连接...");
        while (true) {
            if (selector.select() <= 0) {
                continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                if (!key.isValid()) {
                    key.channel().close();
                }

                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    key.interestOpsAnd(~SelectionKey.OP_READ);

                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    String message = new String(byteBuffer.array(), 0, byteBuffer.limit(), "UTF-8");
                    System.out.println(String.format("线程%s，收到客户端信息:%s", Thread.currentThread().getName(), message));

                    socketChannel.register(selector, SelectionKey.OP_WRITE, message);
                } else if (key.isWritable()) {
                    key.interestOpsAnd(~SelectionKey.OP_WRITE);

                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    String message = (String) key.attachment();
                    message = String.format("[%s]", message);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    byteBuffer.put(message.getBytes("UTF-8"));
                    byteBuffer.flip();
                    try {
                        socketChannel.write(byteBuffer);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    } catch (IOException e) {
                        socketChannel.shutdownInput();
                        key.cancel();
                    }
                }
                iter.remove();
            }
        }
    }

}
