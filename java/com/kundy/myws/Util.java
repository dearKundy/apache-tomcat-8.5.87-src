package com.kundy.myws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author kundy
 * @date 2023/5/10 21:38
 */
public class Util {

    public static byte[] longToByteArray(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    public static byte[] readByteArray(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int bytesRead;

        while ((bytesRead = socketChannel.read(buffer)) > 0) {
            buffer.flip();
            byte[] bytes = new byte[bytesRead];
            buffer.get(bytes);
            outputStream.write(bytes);
            buffer.clear();
        }

        if (bytesRead == -1) {
            socketChannel.close();
        }

        return outputStream.toByteArray();
    }
}
