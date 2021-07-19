package com.ixnah.mc.websocket.handler;

import com.ixnah.mc.websocket.util.HttpClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.LinkedList;
import java.util.Queue;

import static com.ixnah.mc.protocol.util.SpinUtil.spinRequireNonNull;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2020/7/8 13:19
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<Object> implements HttpClient {

    private ChannelHandlerContext ctx;
    private final Queue<Promise<HttpResponse>> responsePromiseQueue = new LinkedList<>();

    public HttpClientHandler() {
        super(false);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (this.ctx == null)
            this.ctx = ctx;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        synchronized (responsePromiseQueue) {
            try {
                for (Promise<HttpResponse> promise : responsePromiseQueue) {
                    promise.tryFailure(new RuntimeException("handlerRemoved"));
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse && !responsePromiseQueue.isEmpty()) {
            responsePromiseQueue.poll().setSuccess((HttpResponse) msg);
        }
    }

    @Override
    public Future<HttpResponse> sendRequest(HttpRequest request) {
        spinRequireNonNull(this, "ctx", "HttpClientHandler must add to pipeline");
        Promise<HttpResponse> responsePromise = new DefaultPromise<>(ctx.executor());
        ctx.channel().writeAndFlush(request).addListener(writeFuture -> {
            if (!writeFuture.isSuccess()) {
                responsePromise.setFailure(writeFuture.cause());
            } else {
                responsePromiseQueue.offer(responsePromise);
            }
        });
        return responsePromise;
    }
}
