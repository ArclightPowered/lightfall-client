package com.ixnah.mc.websocket.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

import static com.ixnah.mc.protocol.util.SpinUtil.spinRequireNonNull;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2020/3/29 18:00
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private ChannelPromise handshakeFuture;
    private final WebSocketClientHandshaker handshaker;
    private final List<ByteBuf> frameContents = new ArrayList<>();

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        super(false);
        this.handshaker = handshaker;
    }

    public Future<Void> handshakeFuture() {
        return spinRequireNonNull(this, "handshakeFuture", "WebSocketClientHandler must add to pipeline");
    }

    // 被添加到pipeline后开始进行Websocket握手
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        frameContents.clear();
        ctx.fireChannelInactive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ctx.channel(), (FullHttpResponse) msg);
                handshakeFuture.trySuccess();
            } catch (Throwable t) {
                t.printStackTrace();
                handshakeFuture.tryFailure(t);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status()
                    + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        handleWebSocketFrame(ctx, (WebSocketFrame) msg);
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        synchronized (frameContents) {
            frameContents.add(frame.content());
            if (frame.isFinalFragment()) {
                ctx.fireChannelRead(Unpooled.wrappedBuffer(frameContents.toArray(new ByteBuf[0])));
                frameContents.clear();
            }
        }
    }
}
