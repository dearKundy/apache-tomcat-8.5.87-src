package com.kundy.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class HttpPostRequest {

    public static void main(String[] args) throws Exception {
        String host = "";
//        String host = "localhost:8090";
        String ip = "127.0.0.1";
        int port = 8090;
        String path = "/";
        String data = "name1=value1&name2=value2";
        String method = "GET";

        // 构建HTTP POST请求报文
        String request = method + " " + path + " HTTP/1.1\r\n"
//                + "Host: " + host + "\r\n"
                + "Content-Type: application/x-www-form-urlencoded\r\n"
                + "Content-Length: " + data.length() + "\r\n"
                + "\r\n"
                + data;

        // 创建Socket连接
        Socket socket = new Socket(ip, port);

        // 获取输出流
        OutputStream outputStream = socket.getOutputStream();

        // 将HTTP请求报文写入输出流中
        outputStream.write(request.getBytes("UTF-8"));
        outputStream.flush();

        // 获取响应
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // 打印响应内容
        System.out.println(response);

        // 关闭连接和输出流
        outputStream.close();
        socket.close();
    }
}