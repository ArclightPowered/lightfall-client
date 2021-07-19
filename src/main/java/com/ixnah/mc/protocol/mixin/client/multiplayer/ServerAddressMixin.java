package com.ixnah.mc.protocol.mixin.client.multiplayer;

import com.ixnah.mc.protocol.UriServerAddress;
import net.minecraft.client.multiplayer.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

import static com.ixnah.mc.protocol.CustomProtocol.URI_REGEX;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 10:53
 */
@Mixin(ServerAddress.class)
public class ServerAddressMixin {

    @Inject(method = "fromString", cancellable = true, at = @At("HEAD"))
    private static void parseCustomProtocol(String addrString, CallbackInfoReturnable<ServerAddress> cir) {
        try {
            if (addrString.matches(URI_REGEX))
                cir.setReturnValue(new UriServerAddress(new URI(addrString.trim())));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
