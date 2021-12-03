package io.izzel.lightfall.client.mixin;

import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientLoginNetHandlerMixin implements ClientLoginNetHandlerBridge {

    @Shadow @Final private Minecraft minecraft;

    private ClientPacketListener lightfall$reuse;

    @Override
    public void bridge$reusePlayHandler(ClientPacketListener handler) {
        this.lightfall$reuse = handler;
    }

    @ModifyArg(method = "handleGameProfile", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setListener(Lnet/minecraft/network/PacketListener;)V"))
    private PacketListener lightfall$reuse(PacketListener origin) {
        if (lightfall$reuse != null) {
            this.minecraft.level = lightfall$reuse.getLevel();
        }
        return lightfall$reuse == null ? origin : lightfall$reuse;
    }
}
