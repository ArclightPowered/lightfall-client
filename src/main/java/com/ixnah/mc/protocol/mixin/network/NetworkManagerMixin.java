package com.ixnah.mc.protocol.mixin.network;

import com.ixnah.mc.protocol.CustomProtocol;
import com.ixnah.mc.protocol.bridge.CustomProtocolBridge;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

import static java.util.Objects.requireNonNull;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 11:56
 */
@Mixin(NetworkManager.class)
public class NetworkManagerMixin implements CustomProtocolBridge {
    @Shadow private Channel channel;

    @Inject(method = "dispatchPacket", at = @At("HEAD"))
    public void onDispatchPacket(IPacket<?> inPacket, GenericFutureListener<? extends Future<? super Void>> futureListeners, CallbackInfo ci) {
        Boolean handshakeCompleted = channel.attr(CustomProtocol.HANDSHAKE_COMPLETED_KEY).get();
        URI serverUri = channel.attr(CustomProtocol.SERVER_URI_KEY).get();
        if (serverUri != null && (handshakeCompleted == null || !handshakeCompleted)) {
            CustomProtocol.accept(serverUri.getScheme().toLowerCase(Locale.ROOT), channel);
        }
        channel.attr(CustomProtocol.HANDSHAKE_COMPLETED_KEY).set(true);
    }

    @Override
    public void setServerUri(URI uri) {
        int count = 0; // handlerAdded() 方法会在netty线程中执行,获取时可能为null,自旋等待
        while (channel == null && count < 1000) {
            LockSupport.parkNanos("executing tasks", 1000L);
            count++;
        }
        requireNonNull(channel, "channel can't be null!");
        channel.attr(CustomProtocol.SERVER_URI_KEY).set(uri);
    }
}
