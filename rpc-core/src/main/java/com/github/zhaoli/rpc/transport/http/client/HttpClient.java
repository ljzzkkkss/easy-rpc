package com.github.zhaoli.rpc.transport.http.client;

import com.github.zhaoli.rpc.transport.api.support.netty.AbstractNettyClient;
import com.github.zhaoli.rpc.transport.http.conveter.HttpClientMessageConverter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhaoli
 * @date 2018/7/19
 */
@Slf4j
public class HttpClient extends AbstractNettyClient {

    @Override
    protected ChannelInitializer initPipeline() {
        HttpClientHandler.init(HttpClient.this, HttpClientMessageConverter.getInstance(getGlobalConfig().getSerializer()));
        log.info("HttpClient initPipeline...");
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline()
                        // 客户端会发出请求，接收响应；也有可能会接收请求（但消息体也是响应,callback）
                        // 服务器会接收请求，发出响应，也有可能会发出请求（但消息体也是响应,callback）
                        .addLast("HttpRequestEncoder", new HttpRequestEncoder())
                        .addLast("HttpResponseDecoder", new HttpResponseDecoder())
                        .addLast("HttpObjectAggregator",new HttpObjectAggregator(10*1024*1024))
                        .addLast("HttpClientHandler", HttpClientHandler.getInstance());
            }
        };
    }

    @Override
    protected HttpClientMessageConverter initConverter() {
        return HttpClientMessageConverter.getInstance(getGlobalConfig().getSerializer());
    }


}
