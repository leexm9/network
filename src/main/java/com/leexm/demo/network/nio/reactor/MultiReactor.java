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
 * 多 Reactor 模式
 * 主 Selector 只负责接收连接，并将连接注册到从 Selector 上
 * 从 Selector 负责具体连接的 I/O 读写、数据处理等
 *
 * @author leexm
 * @date 2019-10-27 16:42
 */
public class MultiReactor {

    private static final int CORE = 4;

    /**
     * 主 Reactor，接收连接，把 SocketChannel 注册到从 Reactor 上
     */
    private final Selector selector;

    private final ServerSocketChannel serverChannel;

    /**
     * 从 selector，用于处理 I/O，可使用 Handler 和  ThreadPoolHandler 两种处理方式
     */
    private final Reactor[] reactors = new Reactor[CORE];

    public MultiReactor(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));

        this.selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, new Acceptor());

        for (int i = 0; i < reactors.length; i++) {
            reactors[i] = new Reactor();
        }
    }

    public void kickOff() {
        System.out.println("服务器启动，等待连接中......");
        for (int i = 0; i < reactors.length; i++) {
            Thread thread = new Thread(reactors[i], "Reactor-" + i);
            thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
                System.out.println("非I/O异常");
                e.printStackTrace();
            });
            thread.start();
        }
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

    private int next = 0;

    private class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                SocketChannel socketChannel = serverChannel.accept();
                if (socketChannel != null) {
                    reactors[next].register(socketChannel);
                    if (++next == CORE) {
                        next = 0;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Reactor implements Runnable {

        private final Selector selector;

        Reactor() throws IOException {
            this.selector = Selector.open();
        }

        void register(SocketChannel socketChannel) {
            try {
                new Handler(selector, socketChannel);
//                new ThreadPoolHandler(selector, socketChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            System.out.println("从 Reactor 启动，等待分配连接.....");
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
            if (runnable != null) {
                runnable.run();
            }
        }
    }

}
