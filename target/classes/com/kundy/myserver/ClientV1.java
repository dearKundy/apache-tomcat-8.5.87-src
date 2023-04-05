package com.kundy.myserver;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class ClientV1 {

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < 10; i++) {
//            Thread.sleep(50);
            new Thread(() -> {

                try {
                    Socket s = new Socket("127.0.0.1", 8888);

                    //构建IO
                    InputStream is = s.getInputStream();
                    OutputStream os = s.getOutputStream();

                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                    //向服务器端发送一条消息
                    bw.write("你好我是小里\n");
                    bw.flush();

                    //读取服务器返回的消息
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String mess = br.readLine();
                    System.out.println(new Date() + "服务器：" + mess);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();
        }

    }
}