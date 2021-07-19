package com.ixnah.mc.websocket.handler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/19 13:07
 *
 * 如 {@link net.minecraft.network.NettyPacketEncoder} 等Encoder没有使用注解Sharable, 在移除一次后不能再次添加, 所以使用包装类解决
 */
@Sharable
public class EncoderWrapper<T> extends ChannelOutboundHandlerAdapter {

    private final MessageToByteEncoder<T> encoder;

    public EncoderWrapper(MessageToByteEncoder<T> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        encoder.write(ctx, msg, promise);
    }
}
