package com.kundy.mynio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioServer {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        // 非阻塞
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 阻塞等待就绪事件
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isAcceptable()) {
                    ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();

                    SocketChannel clientSocket = serverSocket.accept();

                    // 非阻塞
                    clientSocket.configureBlocking(false);

                    // 注册读事件
                    clientSocket.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    SocketChannel clientSocket = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);

                    int len = clientSocket.read(buffer);

                    System.out.println("Received message: " + new String(buffer.array(), 0, len));

                    // 注册写事件
                    clientSocket.register(selector, SelectionKey.OP_WRITE);
                } else if (key.isWritable()) {
                    SocketChannel clientSocket = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    buffer.put("Hello, client".getBytes());
                    buffer.flip();

                    clientSocket.write(buffer);

                    // 注册读事件
                    clientSocket.register(selector, SelectionKey.OP_READ);
                }

                keyIterator.remove();
            }
        }
    }
}