package com.ixnah.mc.protocol;

import com.ixnah.mc.websocket.WebSocketConsumer;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 16:19
 */
public class CustomProtocol {

    public static final AttributeKey<Boolean> HANDSHAKE_COMPLETED_KEY = AttributeKey.valueOf("CustomProtocolHandshakeCompleted");
    public static final AttributeKey<URI> SERVER_URI_KEY = AttributeKey.valueOf("CustomProtocolServerUri");
    public static final String URI_REGEX = "[a-zA-z]+://[^\\s]*";

    private static final Map<String, Class<? extends Consumer<Channel>>> protocolMap = new ConcurrentHashMap<>(16);
    private static final Map<String, Integer> defaultPortMap = new ConcurrentHashMap<>(16);

    public static void accept(String name, Channel channel) {
        Consumer<Channel> channelConsumer = (ch) -> { };
        try {
            Class<? extends Consumer<Channel>> channelConsumerClass = protocolMap.get(name);
            if (channelConsumerClass != null) {
                channelConsumer = channelConsumerClass.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        channelConsumer.accept(channel);
    }

    public static void register(Class<? extends Consumer<Channel>> channelConsumer, String... names) {
        for (String name : names) {
            protocolMap.put(name, channelConsumer);
        }
    }

    public static Class<? extends Consumer<Channel>> unregister(String name) {
        return protocolMap.remove(name);
    }

    public static void setDefaultPort(int port, String... names) {
        for (String name : names) {
            defaultPortMap.put(name, port);
        }
    }

    public static int getServerUriPort(URI serverUri) {
        int uriPort = serverUri.getPort();
        if (uriPort != -1) {
            return uriPort;
        }
        String name = serverUri.getScheme();
        if (name != null) {
            name = name.toLowerCase(Locale.ROOT);
        }
        return defaultPortMap.getOrDefault(name, 25565);
    }

    static {
        register(WebSocketConsumer.class, "ws", "wss", "http", "https");
        setDefaultPort(80, "ws", "http");
        setDefaultPort(443, "wss", "https");
        setDefaultPort(25565, "tcp", "mc", "minecraft");
    }
}
