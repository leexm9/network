package com.leexm.demo.network.nio.reactor.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * 单线程版
 *
 * @author leexm
 * @date 2019-10-26 16:57
 */
public class Handler implements Runnable {

    /**
     * 定义 handler 可选的状态变量
     */
    static final int READING = 0, SENDING = 1;

    final SocketChannel socketChannel;

    final SelectionKey selectionKey;

    /**
     * 设置初始状态，读取
     */
    int state = READING;

    ByteBuffer input = ByteBuffer.allocate(1024);

    ByteBuffer output = ByteBuffer.allocate(1024);

    public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            if (state == READING) {
                read();
            } else if (state == SENDING) {
                send();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void read() throws IOException {
        int len = socketChannel.read(input);
        if (inputIsComplete(len)) {
            process();
            state = SENDING;
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    void send() throws IOException {
        socketChannel.write(output);
        if (outputIsComplete()) {
            socketChannel.close();
        }
    }

    /**
     * 判断数据是否读完
     * @return
     */
    boolean inputIsComplete(int len) {
        return len < 0;
    }

    boolean outputIsComplete() {
        // out bytebuffer 数据写完即完成写
        return !output.hasRemaining();
    }

    /**
     * 自定义处理逻辑
     */
    void process() {
        input.flip();
        String message = null;
        try {
            message = new String(input.array(), 0, input.limit(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        System.out.println(String.format("线程%s，收到客户端信息:%s", Thread.currentThread().getName(), message));
        output.put((byte) '[');
        output.put(input);
        output.put((byte) ']');
        output.flip();
    }

}
