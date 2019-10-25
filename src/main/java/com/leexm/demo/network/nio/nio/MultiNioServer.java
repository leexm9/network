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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * nio实现服务端多线程/线程池版本
 *
 * @author leexm
 * @date 2019-10-25 22:53
 */
public class MultiNioServer {

    private static final int PORT = 8088;

    private static AtomicInteger atomicInteger = new AtomicInteger();

    public static void main(String[] args) throws IOException {
        // 定义线程池
        ExecutorService executors = Executors.newFixedThreadPool(5, (Runnable r) -> {
            Thread thread = new Thread(r, "server-" + atomicInteger.incrementAndGet());
            thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
                System.out.println("非I/O异常");
                e.printStackTrace();
            });
            return thread;
        });

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
                // I/O 连接放在主线程处理
                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 这里线程池处理，如果处理耗时较久，主线程已经进行了下一轮的 selector.selectedKeys() 时，该 SelectionKey 有可能又被选中
                    // 导致：主线程得到是 ready 的 key 其实其他线程已经将其 cancel 了，再此进入这个分支会抛出 SocketCanceledException
                    // 这里将 selectionKey 的读事件去除，使已经在处理的 selectionKey 不会因为线程处理比较耗时，导致主线程一直将其select 出来
                    key.interestOpsAnd(~SelectionKey.OP_READ);

                    executors.execute(() -> {
                        try {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                            socketChannel.read(byteBuffer);
                            byteBuffer.flip();
                            String message = new String(byteBuffer.array(), 0, byteBuffer.limit(), "UTF-8");
                            System.out.println(String.format("线程%s，收到客户端信息:%s", Thread.currentThread().getName(), message));

                            message = String.format("[%s]", message);
                            byteBuffer.clear();
                            byteBuffer.put(message.getBytes("UTF-8"));
                            byteBuffer.flip();
                            socketChannel.write(byteBuffer);
                            socketChannel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                iter.remove();
            }
        }
    }

}
