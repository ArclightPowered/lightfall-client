package io.izzel.lightfall.client;

import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import io.izzel.lightfall.client.gui.LightfallHandshakeScreen;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.login.client.CCustomPayloadLoginPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.registries.GameData;

import java.nio.charset.StandardCharsets;

@Mod("lightfallclient")
public class LightfallClient {

    private static final byte[] RESET_ACK = "lightfall:ack".getBytes(StandardCharsets.UTF_8);

    public LightfallClient() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerChannel);
    }

    private void registerChannel() {
        EventNetworkChannel channel = NetworkRegistry.newEventChannel(
            new ResourceLocation("lightfall", "reset"),
            () -> "1", s -> true, s -> true
        );
        channel.addListener(this::handleReset);
    }

    private void handleReset(NetworkEvent.ServerCustomPayloadEvent event) {
        NetworkEvent.Context context = event.getSource().get();
        NetworkManager netManager = context.getNetworkManager();
        if (netManager == null || !(netManager.getNetHandler() instanceof ClientPlayNetHandler)) {
            return;
        }
        context.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            LightfallHandshakeScreen screen = new LightfallHandshakeScreen(netManager);
            client.displayGuiScreen(screen);
            if (client.world != null) {
                GameData.revertToFrozen();
                client.world = null;
            }
            netManager.setConnectionState(ProtocolType.LOGIN);
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(RESET_ACK));
            netManager.sendPacket(new CCustomPayloadLoginPacket(0x11FFA1, buffer));
            ClientLoginNetHandler netHandler = new ClientLoginNetHandler(netManager, client, new MultiplayerScreen(new MainMenuScreen()), screen::displaySavingString);
            ((ClientLoginNetHandlerBridge) netHandler).bridge$reusePlayHandler((ClientPlayNetHandler) netManager.getNetHandler());
            netManager.setNetHandler(netHandler);
        });
        context.setPacketHandled(true);
    }
}
