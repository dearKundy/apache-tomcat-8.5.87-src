package com.kundy.mynio;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class BioServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8181);

        while (true) {
            // 监听客户端连接，阻塞
            Socket clientSocket = serverSocket.accept();
            System.out.println(String.valueOf(new Date()) + InetAddress.getLocalHost() + "已连接到服务器");
            // 获取输入流，阻塞
            InputStream inputStream = clientSocket.getInputStream();

            byte[] buffer = new byte[1024];
            // 读取输入流数据，阻塞
            int len = inputStream.read(buffer);

            System.out.println("Received message: " + new String(buffer, 0, len));
        }
    }
}