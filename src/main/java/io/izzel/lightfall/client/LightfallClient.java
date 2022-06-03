package io.izzel.lightfall.client;

import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import io.izzel.lightfall.client.gui.LightfallHandshakeScreen;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.registries.GameData;

import java.nio.charset.StandardCharsets;

@Mod("lightfallclient")
public class LightfallClient {

    private static final byte[] RESET_ACK = "lightfall:ack".getBytes(StandardCharsets.UTF_8);

    public LightfallClient() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientSetup::registerChannel);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
    }

    public static class ClientSetup {

        private static void registerChannel() {
            var channel = NetworkRegistry.newEventChannel(
                new ResourceLocation("lightfall", "reset"),
                () -> "1", s -> true, s -> true
            );
            channel.addListener(ClientSetup::handleReset);
        }

        private static void handleReset(NetworkEvent.ServerCustomPayloadEvent event) {
            var context = event.getSource().get();
            var netManager = context.getNetworkManager();
            if (netManager == null || !(netManager.getPacketListener() instanceof ClientGamePacketListener)) {
                return;
            }
            context.enqueueWork(() -> {
                var client = Minecraft.getInstance();
                var screen = new LightfallHandshakeScreen(netManager);
                client.setScreen(screen);
                if (client.level != null) {
                    GameData.revertToFrozen();
                    client.level = null;
                }
                netManager.setProtocol(ConnectionProtocol.LOGIN);
                var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(RESET_ACK));
                netManager.send(new ServerboundCustomQueryPacket(0x11FFA1, buffer));
                var netHandler = new ClientHandshakePacketListenerImpl(netManager, client, new JoinMultiplayerScreen(new TitleScreen()), screen::setComponent);
                ((ClientLoginNetHandlerBridge) netHandler).bridge$reusePlayHandler((ClientPacketListener) netManager.getPacketListener());
                netManager.setListener(netHandler);
            });
            context.setPacketHandled(true);
        }
    }
}
