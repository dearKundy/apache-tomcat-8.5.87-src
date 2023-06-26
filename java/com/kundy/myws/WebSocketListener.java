package com.kundy.myws;

import java.io.IOException;

/**
 * @author kundy
 * @date 2023/5/10 16:42
 */
public interface WebSocketListener {

    void onOpen(ClientSession session) throws IOException;

    void onMessage(ClientSession session) throws IOException;

    void onException(ClientSession session, Exception ex);

    void onClose(ClientSession session) throws IOException;

}
