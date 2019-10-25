package com.leexm.demo.network.bio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务端多线程实现
 *
 * @author leexm
 * @date 2019-10-24 23:40
 */
public class SocketServer {

    private static final int PORT = 8088;

    private static AtomicInteger atomicInteger = new AtomicInteger();

    public static void main(String[] args) throws IOException {
        ExecutorService executors = Executors.newFixedThreadPool(5, (Runnable r) -> {
            Thread thread = new Thread(r, "server-" + atomicInteger.incrementAndGet());
            thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
                System.out.println("非I/O异常");
                e.printStackTrace();
            });
            return thread;
        });

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(PORT));

        System.out.println("服务器启动，等待客户端连接...");
        while (true) {
            Socket socket = serverSocket.accept();

            executors.execute(() -> {
                try {
                    InputStream inputStream = socket.getInputStream();
                    byte[] bytes = inputStream.readAllBytes();
                    String message = new String(bytes, "UTF-8");
                    System.out.println(String.format("线程%s，收到客户端信息:%s", Thread.currentThread().getName(), message));

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(String.format("[%s]", message).getBytes("UTF-8"));

                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }
    }

}
