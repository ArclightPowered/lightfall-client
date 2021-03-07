package io.izzel.lightfall.client.mixin;

import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.INetHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientLoginNetHandler.class)
public class ClientLoginNetHandlerMixin implements ClientLoginNetHandlerBridge {

    @Shadow @Final private Minecraft mc;

    private ClientPlayNetHandler lightfall$reuse;

    @Override
    public void bridge$reusePlayHandler(ClientPlayNetHandler handler) {
        this.lightfall$reuse = handler;
    }

    @ModifyArg(method = "handleLoginSuccess", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;setNetHandler(Lnet/minecraft/network/INetHandler;)V"))
    private INetHandler lightfall$reuse(INetHandler origin) {
        if (lightfall$reuse != null) {
            this.mc.world = lightfall$reuse.getWorld();
        }
        return lightfall$reuse == null ? origin : lightfall$reuse;
    }
}
