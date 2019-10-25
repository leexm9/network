package com.leexm.demo.network.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * @author leexm
 * @date 2019-10-24 23:40
 */
public class SocketClient {

    private static final int PORT = 8088;

    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        byte[] message = "Hello, world!".getBytes("UTF-8");
        int count = 10;
        CountDownLatch countDownLatch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(HOST, PORT));

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message);
                    // 通知对端，写已完成
                    socket.shutdownOutput();

                    // 使用的 jdk11，jdk8 中没有该 api
                    InputStream inputStream = socket.getInputStream();
                    byte[] bytes = inputStream.readAllBytes();
                    String response = new String(bytes, "UTF-8");
                    System.out.println(String.format("[%s]线程服务端响应:%s", Thread.currentThread().getName(), response));
                    socket.close();

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
