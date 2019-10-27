package com.leexm.demo.network.nio.reactor.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 多线程处理
 *
 * @author leexm
 * @date 2019-10-26 23:15
 */
public class ThreadPoolHandler extends Handler {

    private static final int PROCESSING = 2;

    private final Selector selector;

    private static final ExecutorService executors = Executors.newFixedThreadPool(4, (Runnable r) -> {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            System.out.println("非I/O异常");
            e.printStackTrace();
        });
        return thread;
    });

    public ThreadPoolHandler(Selector selector, SocketChannel socketChannel) throws IOException {
        super(selector, socketChannel);
        this.selector = selector;
    }

    @Override
    protected void read() throws IOException {
        int len = socketChannel.read(input);
        if (inputIsComplete(len)) {
            // 取消 read，防止数据处理耗时时 Selector将该 key 的 read 事件频繁取出
            selectionKey.interestOpsAnd(~SelectionKey.OP_READ);
            state = PROCESSING;
            // 数据处理可能是 CPU 密集型
            executors.execute(() -> {
                try {
                    processAndHandleOff();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void processAndHandleOff() throws InterruptedException {
        process();
        if (Thread.currentThread().getName().equals("Thread-1")) {
            // 模拟CPU耗时操作
            TimeUnit.MILLISECONDS.sleep(10);
        }
        state = SENDING;
        selectionKey.interestOps(SelectionKey.OP_WRITE);
        selector.wakeup();
    }

}
