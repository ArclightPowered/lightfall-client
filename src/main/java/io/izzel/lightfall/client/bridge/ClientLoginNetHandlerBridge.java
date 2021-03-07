package io.izzel.lightfall.client.bridge;

import net.minecraft.client.network.play.ClientPlayNetHandler;

public interface ClientLoginNetHandlerBridge {

    void bridge$reusePlayHandler(ClientPlayNetHandler handler);
}
