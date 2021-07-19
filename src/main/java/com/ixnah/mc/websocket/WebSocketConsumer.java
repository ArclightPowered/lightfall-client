package com.ixnah.mc.websocket;

import com.ixnah.mc.protocol.CustomProtocol;
import com.ixnah.mc.websocket.handler.EncoderWrapper;
import com.ixnah.mc.websocket.handler.HttpClientHandler;
import com.ixnah.mc.websocket.handler.PacketToFrameHandler;
import com.ixnah.mc.websocket.handler.WebSocketClientHandler;
import com.ixnah.mc.websocket.util.ManifestUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MinecraftVersion;

import javax.net.ssl.SSLException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
    public static final String USER_AGENT_FORMAT = "LightfallClient/1.0.0 Minecraft/%s Netty/%s Java/%s";
    private static final String PACKET_HANDLER_NAME = "packet_handler";
    private final Map<String, Object> removedEncoder = new LinkedHashMap<>(8);
    private Channel channel;

    /**
     * 由于 {@link WebSocketClientHandshaker#handshake(io.netty.channel.Channel, io.netty.channel.ChannelPromise)}
     * 发送HTTP握手包会直接调用 {@link Channel#writeAndFlush(java.lang.Object)}, 所以说必须在WebSocket握手前
     * 把 {@link MessageToByteEncoder} 从pipeline中临时移除.
     */
    @Override
    public void accept(Channel channel) {
        this.channel = channel;
        cleanPipeline();
        handshake();
        resetPipeline();
    }

    private void cleanPipeline() {
        channel.pipeline().toMap().forEach((name, handler) -> {
            if (handler instanceof MessageToByteEncoder) {
                MessageToByteEncoder<?> encoder = (MessageToByteEncoder<?>) handler;
                if (encoder.isSharable()) {
                    removedEncoder.put(name, encoder);
                } else {
                    try {
                        MethodType methodType = MethodType.methodType(void.class);
                        MethodHandle handle = MethodHandles.lookup().findConstructor(MessageToByteEncoder.class, methodType);
                        removedEncoder.put(name, handle);
                    } catch (NoSuchMethodException | IllegalAccessException exception) {
                        removedEncoder.put(name, new EncoderWrapper<>(encoder));
                    }
                }
                channel.pipeline().remove(encoder);
            }
        });
    }

    private void resetPipeline() {
        removedEncoder.forEach((name, encoder) -> {
            if (encoder instanceof ChannelHandler) {
                channel.pipeline().addBefore(PACKET_HANDLER_NAME, name, (ChannelHandler) encoder);
            } else {
                MethodHandle encoderConstructor = (MethodHandle) encoder;
                try {
                    MessageToByteEncoder<?> newEncoder = (MessageToByteEncoder<?>) encoderConstructor.invoke();
                    channel.pipeline().addBefore(PACKET_HANDLER_NAME, name, newEncoder);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        removedEncoder.clear();
    }

    private void handshake() {
        URI serverUri = channel.attr(CustomProtocol.SERVER_URI_KEY).get();
        String scheme = serverUri.getScheme().toLowerCase(Locale.ROOT).replace("ws", "http");
        boolean useSSL = scheme.equals("https");
        if (useSSL) {
            try {
                SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                channel.pipeline().addFirst("SSL", sslContext.newHandler(channel.alloc())); // TODO: SSL证书验证
            } catch (SSLException e) {
                e.printStackTrace();
            }
        }
        HttpClientHandler httpClient = new HttpClientHandler();
        channel.pipeline()
                .addAfter("timeout", "HttpClientHandler", httpClient)
                .addAfter("timeout", "HttpObjectAggregator", new HttpObjectAggregator(8192))
                .addAfter("timeout", "HttpClientCodec", new HttpClientCodec());

        String host = serverUri.getHost();
        if (serverUri.getPort() != -1) {
            host = host + ":" + serverUri.getPort();
        }
        String minecraftVersion = MinecraftVersion.GAME_VERSION.getName();
        String nettyVersion = ManifestUtil.getValue(Channel.class, "Implementation-Version");
        String javaVersion = System.getProperty("java.version");
        String userAgent = String.format(USER_AGENT_FORMAT, minecraftVersion, nettyVersion, javaVersion);
        String playerId = Minecraft.getInstance().getSession().getPlayerID();

        String serverPath = serverUri.getPath();
        if (serverPath == null || serverPath.trim().isEmpty()) {
            serverPath = "/";
        } else {
            serverPath = serverUri.toASCIIString().split(host, 2)[1];
        }
        HttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, serverPath);
        request.headers()
                .add(HOST, host)
                .add(ACCEPT, "*/*")
                .add(USER_AGENT, userAgent)
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
        if (response.status().code() >= HttpResponseStatus.BAD_REQUEST.code()) { // TODO: 可能出现的重定向 301 302
            RuntimeException exception = new RuntimeException(response.status().toString());
            exception.printStackTrace();
            throw exception;
        }

        String token = ((FullHttpResponse) response).content().toString(Charset.defaultCharset());
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add(USER_AGENT, userAgent).add(AUTHORIZATION, token);
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
        channel.pipeline().addBefore(PACKET_HANDLER_NAME, "PacketToFrameHandler", new PacketToFrameHandler());
    }
}
