package com.ixnah.mc.protocol.mixin.client.network;

import com.ixnah.mc.protocol.UriServerAddress;
import com.ixnah.mc.protocol.bridge.CustomProtocolBridge;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 8:44
 */
@Mixin(ServerPinger.class)
public class ServerPingerMixin {

    @Inject(method = "ping", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;"))
    private void connectCustomProtocol(ServerData server, Runnable p_147224_2_, CallbackInfo ci, ServerAddress serveraddress, NetworkManager networkmanager) {
        if (serveraddress instanceof UriServerAddress) {
            UriServerAddress uriServerAddress = (UriServerAddress) serveraddress;
            ((CustomProtocolBridge) networkmanager).setServerUri(uriServerAddress.getAddress());
        }
    }
}
