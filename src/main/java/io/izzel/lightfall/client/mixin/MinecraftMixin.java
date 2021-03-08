package io.izzel.lightfall.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public ClientPlayerEntity player;

    @Redirect(method = "getBackgroundMusicSelector", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/client/world/ClientWorld;"))
    private ClientWorld lightfall$usePlayerWorld(Minecraft minecraft) {
        return (ClientWorld) this.player.world;
    }
}
