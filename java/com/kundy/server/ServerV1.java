package com.kundy.server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerV1 {

    public static void main(String[] args) throws Exception {

        // 设置最大连接数为 3
        int backLog = 4;
        int threadNum = 2;

        // 创建一个线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);

        // backlog 是 ServerSocket 的构造方法中的一个参数，它表示请求队列的最大长度。
        // 请求队列是用于存储连接请求的队列，当一个客户端尝试连接到服务器时，服务器会将该连接请求添加到请求队列中。
        // 当服务器准备处理连接请求时，会从请求队列的头部取出一个连接请求。具体来说，当调用 ServerSocket 的 accept() 方法时，
        // 服务器会从请求队列的头部取出一个连接请求，并将其转换为一个 Socket 对象，以便服务器可以使用该 Socket 对象与客户端进行通信。
        ServerSocket ss = new ServerSocket(8888, backLog);

        while (true) {
            // 简单来说就是不执行 accept 的时候（或者执行慢），队列就会堆积，堆积达到 acceptCount，就拒绝连接了
            // 那什么时候会不执行accept或者执行慢呢？都有可能。。。自己想
            Socket s = ss.accept();
            System.out.println(InetAddress.getLocalHost() + "已连接到服务器");
            // 每从队列中取一个连接，就睡半秒，模拟消费慢
            Thread.sleep(500);
            // 向线程池中提交任务，一次只能处理threadNum个线程，剩余的等待
            executor.submit(() -> {
                try {
                    Thread.sleep(5000);
                    BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    //读取客户端发送来的消息
                    String mess = br.readLine();
                    System.out.println("客户端：" + mess);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    bw.write("感谢支持" + "\n");
                    bw.flush();
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });

        }

    }
}