package com.leexm.demo.network.nio.reactor;

import com.leexm.demo.network.nio.reactor.handler.Handler;
import com.leexm.demo.network.nio.reactor.handler.ThreadPoolHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 经典 Reactor 模型，I/O 读写主线程负责
 * 根据 handler 的不同实现，数据处理可以和 I/O 读写在一个线程内或使用另外的线程处理
 *
 * @author leexm
 * @date 2019-10-26 16:31
 */
public class Reactor {

    private final Selector selector;

    private final ServerSocketChannel serverChannel;

    public Reactor(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());
    }

    public void kickOff() {
        System.out.println("服务器启动，等待连接中......");
        try {
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    this.dispatch(iter.next());
                }
                selectedKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey selectionKey) {
        Runnable runnable = (Runnable) selectionKey.attachment();
        // 这里是同步调用
        if (runnable != null) {
            runnable.run();
        }
    }

    private class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverChannel.accept();
                if (socketChannel != null) {
//                    new Handler(selector, socketChannel); // 同一个线程进行 I/O 读写和数据处理
                    new ThreadPoolHandler(selector, socketChannel);     // 线程池处理数据，防止影响 I/O 读写
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
