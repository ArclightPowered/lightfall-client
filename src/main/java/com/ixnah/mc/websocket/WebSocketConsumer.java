package com.ixnah.mc.websocket;

import com.ixnah.mc.protocol.CustomProtocol;
import com.ixnah.mc.websocket.handler.HttpClientHandler;
import com.ixnah.mc.websocket.handler.PacketToFrameHandler;
import com.ixnah.mc.websocket.handler.WebSocketClientHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.network.*;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 13:07
 */
public class WebSocketConsumer implements Consumer<Channel> {
    public static final String USER_AGENT_VALUE = "Java Netty/4 Minecraft LightfallClient/1.0.0";
    private Channel channel;

    @Override
    public void accept(Channel channel) {
        this.channel = channel;
        cleanPipeline();
        handshake();
        resetPipeline();
    }

    private void cleanPipeline() {
        channel.pipeline().names().forEach(name -> {
            if (name.startsWith("DefaultChannelPipeline$TailContext") || name.equals("packet_handler")) return;
            channel.pipeline().remove(name);
        });
    }

    private void resetPipeline() {
        channel.pipeline()
                .addFirst("timeout", new ReadTimeoutHandler(30))
                .addBefore("packet_handler", "splitter", new NettyVarint21FrameDecoder())
                .addBefore("packet_handler", "decoder", new NettyPacketDecoder(PacketDirection.CLIENTBOUND))
                .addBefore("packet_handler", "prepender", new NettyVarint21FrameEncoder())
                .addBefore("packet_handler", "encoder", new NettyPacketEncoder(PacketDirection.SERVERBOUND));
    }

    private void handshake() {
        URI serverUri = channel.attr(CustomProtocol.SERVER_URI_KEY).get();
        String scheme = serverUri.getScheme().replace("ws", "http");
        boolean useSSL = scheme.equalsIgnoreCase("https");
        if (useSSL) {
            try {
                SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                channel.pipeline().addLast("SSL", sslContext.newHandler(channel.alloc()));
            } catch (SSLException e) {
                e.printStackTrace();
            }
        }
        HttpClientHandler httpClient = new HttpClientHandler();
        channel.pipeline()
                .addBefore("packet_handler", "HttpClientCodec", new HttpClientCodec())
                .addBefore("packet_handler", "HttpObjectAggregator", new HttpObjectAggregator(8192))
                .addBefore("packet_handler", "HttpClientHandler", httpClient);

        String playerId = Minecraft.getInstance().getSession().getPlayerID();
        String host = serverUri.getHost();
        if (serverUri.getPort() != -1)
            host = host + ":" + serverUri.getPort();

        HttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, serverUri.getPath());
        request.headers()
                .add(HOST, host)
                .add(USER_AGENT, USER_AGENT_VALUE)
                .add(PRAGMA, NO_CACHE)
                .add(CACHE_CONTROL, NO_STORE) // 设置CDN不缓存
                .add(CONNECTION, KEEP_ALIVE) // 设置长链接
                .add(AUTHORIZATION, playerId); // 传递UUID

        Future<HttpResponse> responseFuture = httpClient.sendRequest(request).syncUninterruptibly();
        if (!responseFuture.isSuccess()) {
            responseFuture.cause().printStackTrace();
            throw new RuntimeException(responseFuture.cause());
        }
        HttpResponse response = responseFuture.getNow();
        if (response.status().code() >= 400) {
            RuntimeException exception = new RuntimeException(response.status().toString());
            exception.printStackTrace();
            throw exception;
        }

        String token = ((FullHttpResponse) response).content().toString(Charset.defaultCharset());
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(AUTHORIZATION, token);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                .newHandshaker(serverUri, WebSocketVersion.V13, "Minecraft", true, headers);
        WebSocketClientHandler webSocketClientHandler = new WebSocketClientHandler(handshaker);
        channel.pipeline().replace("HttpClientHandler", "WebSocketClientHandler", webSocketClientHandler); // 发送WebSocket握手请求
        Future<?> handshakeFuture = webSocketClientHandler.handshakeFuture().syncUninterruptibly(); // 阻塞: 等待握手结束
        if (!handshakeFuture.isSuccess()) { // 握手失败 抛出异常退出连接
            handshakeFuture.cause().printStackTrace();
            throw new RuntimeException(handshakeFuture.cause());
        }
        // 握手成功
        channel.pipeline().addBefore("packet_handler", "PacketToFrameHandler", new PacketToFrameHandler());
    }
}
