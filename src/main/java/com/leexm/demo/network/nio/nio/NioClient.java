package com.leexm.demo.network.nio.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

/**
 * @author lxm
 * @date 2019/10/25 13:09
 */
public class NioClient {

    private static final int PORT = 8088;

    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws IOException, InterruptedException {
        int count = 5;
        ByteBuffer message = ByteBuffer.wrap("Hello, world!".getBytes("UTF-8"));
        CountDownLatch countDownLatch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.connect(new InetSocketAddress(HOST, PORT));

                    socketChannel.write(message);
                    socketChannel.shutdownOutput();

                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    socketChannel.read(byteBuffer);
                    byteBuffer.flip();
                    String response = new String(byteBuffer.array(), 0, byteBuffer.limit(), "UTF-8");
                    System.out.println(String.format("[%s]线程服务端响应:%s", Thread.currentThread().getName(), response));
                    socketChannel.close();

                    countDownLatch.countDown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "Sender-" + i).start();
        }

        countDownLatch.await();
        System.out.println(String.format("%d 个请求收发完成!", count));
    }

}
