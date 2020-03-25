package com.github.zhaoli.rpc.transport.http.client;

import com.github.zhaoli.rpc.transport.api.Client;
import com.github.zhaoli.rpc.transport.api.converter.ClientMessageConverter;
import com.github.zhaoli.rpc.common.domain.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhaoli
 * @date 2018/7/19
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpClientHandler extends ChannelInboundHandlerAdapter {
    private Client client;
    private ClientMessageConverter converter;

    private static HttpClientHandler INSTANCE;

    private HttpClientHandler(Client client, ClientMessageConverter converter) {
        this.client = client;
        this.converter = converter;
    }

    public synchronized static void init(Client client, ClientMessageConverter converter) {
        if (INSTANCE == null) {
            INSTANCE = new HttpClientHandler(client, converter);
        }
    }

    public static HttpClientHandler getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("instance did not initialize");
        }
        return INSTANCE;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端与服务器{}通道已开启...", client.getServiceURL().getAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = converter.convertResponse2Message(msg);
        log.info("接收到服务器 {} 响应: {}", client.getServiceURL().getAddress(), message.getResponse());
        if (message.getType() == Message.RESPONSE) {
            client.handleRPCResponse(message.getResponse());
        } else if (message.getType() == Message.REQUEST) {
            client.handleCallbackRequest(message.getRequest(), ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        client.handleException(cause);
    }
}
