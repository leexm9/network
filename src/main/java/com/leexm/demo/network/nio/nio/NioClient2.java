package com.leexm.demo.network.nio.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 客户端重复给服务器发送数据
 *
 * @author leexm
 * @date 2019-12-10 22:08
 */
public class NioClient2 {

    private static final int PORT = 8088;

    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        socketChannel.connect(new InetSocketAddress(HOST, PORT));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);

        int i = 2;
        while (true) {
            if (selector.select() <= 0) {
                continue;
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                SocketChannel socketChannel1 = (SocketChannel) key.channel();
                if (!key.isValid()) {
                    key.channel().close();
                }
                // OP_CONNECT 两种情况，连接成功或失败这个方法都会返回true
                if (key.isConnectable()) {
                    // 由于非阻塞模式，connect只管发起连接请求，finishConnect()方法会阻塞到链接结束并返回是否成功
                    // 另外还有一个isConnectionPending()返回的是是否处于正在连接状态(还在三次握手中)
                    if (socketChannel1.finishConnect()) {
                        ByteBuffer message = ByteBuffer.wrap("Hello, world!".getBytes("UTF-8"));
                        socketChannel1.write(message);
                    } else {
                        System.out.println("客户端连接失败");
                    }
                    socketChannel1.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    String response = new String(byteBuffer.array(), 0, byteBuffer.limit(), "UTF-8");
                    System.out.println(String.format("[%s]线程服务端响应:%s", Thread.currentThread().getName(), response));
                    if (--i <= 0) {
                        socketChannel1.close();
                    } else {
                        socketChannel1.register(selector, SelectionKey.OP_WRITE);
                    }
                } else if (key.isWritable()) {
                    ByteBuffer message = ByteBuffer.wrap("Hello, world!".getBytes("UTF-8"));
                    socketChannel1.write(message);
                    socketChannel1.register(selector, SelectionKey.OP_READ);
                }
                iter.remove();
            }
        }
    }

}
