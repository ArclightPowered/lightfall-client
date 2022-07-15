package io.izzel.lightfall.client.mixin;

import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.GameData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void lightfall$resetState(Component p_104543_, CallbackInfo ci) {
        if (lightfall$reuse != null && lightfall$reuse.getLevel() != null) {
            this.minecraft.level = lightfall$reuse.getLevel();
            this.minecraft.clearLevel();
        } else {
            GameData.revertToFrozen();
        }
    }
}
