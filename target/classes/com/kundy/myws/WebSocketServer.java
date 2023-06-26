package com.kundy.myws;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * https://blog.csdn.net/Kurozaki_Kun/article/details/78843783
 *
 * @author kundy
 * @date 2023/5/9 21:21
 */
public class WebSocketServer {

    private Selector serverSelector;

    private WebSocketListener socketListener;

    private boolean isRunning = true;

    public WebSocketServer(int serverPort, WebSocketListener socketListener) throws Exception {

        //初始化ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(serverPort));
        serverSocketChannel.configureBlocking(false);

        //创建选择器
        serverSelector = Selector.open();

        //注册ServerSocketChannel的ACCEPT事件至选择器
        serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
        this.socketListener = socketListener;

    }

    public void run() throws IOException {
        while (isRunning) {
            int selectCount = serverSelector.select();
            if (selectCount == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = serverSelector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectKey = iterator.next();

                if (selectKey.isAcceptable()) {

                    // ACCEPT就绪，此时调用ServerSocketChannel的accept()方法可获得连接的SocketChannel对象，将其READ事件注册到选择器，就可以读取内容了。
                    ServerSocketChannel serverChannel = (ServerSocketChannel) selectKey.channel();
                    SocketChannel acceptSocketChannel = serverChannel.accept();
                    // 设置为非阻塞模式
                    acceptSocketChannel.configureBlocking(false);
                    acceptSocketChannel.register(serverSelector, SelectionKey.OP_READ);

                } else if (selectKey.isReadable()) {
                    try {
                        SocketChannel socketChannel = (SocketChannel) selectKey.channel();
                        //用前面定义的ClientSession来作为SocketChannel的attach object，方便存储关于SocketChannel的其他信息，容易管理。
                        ClientSession session = (ClientSession) selectKey.attachment();

                        if (session == null) {
                            //如果SocketChannel还没有被ClientSession绑定，认为这是一个新连接，需要完成握手
                            byte[] byteArray = Util.readByteArray(socketChannel);
                            System.out.println(new String(byteArray));
                            WSProtocol.Header header = WSProtocol.Header.decodeFromString(new String(byteArray));
                            String receiveKey = header.getHeader("Sec-WebSocket-Key");
                            String response = WSProtocol.getHandShakeResponse(receiveKey);
                            socketChannel.write(ByteBuffer.wrap(response.getBytes()));

                            ClientSession newSession = new ClientSession(socketChannel);
                            selectKey.attach(newSession);
                            //会话打开
                            socketListener.onOpen(newSession);
                        } else {
                            //收到数据，交给上面定义的接口处理
                            socketListener.onMessage(session);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        //出现异常，进行一系列处理
                        selectKey.channel().close();
                        selectKey.cancel();

                        ClientSession attSession = (ClientSession) selectKey.attachment();
                        //抛出异常
                        socketListener.onException(attSession, e);
                        //强制关闭抛出异常的连接
                        socketListener.onClose(attSession);
                    }

                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new WebSocketServer(8888, new WebSocketListenerImpl()).run();
    }


}
