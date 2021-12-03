package io.izzel.lightfall.client.bridge;


import net.minecraft.client.multiplayer.ClientPacketListener;

public interface ClientLoginNetHandlerBridge {

    void bridge$reusePlayHandler(ClientPacketListener handler);
}
