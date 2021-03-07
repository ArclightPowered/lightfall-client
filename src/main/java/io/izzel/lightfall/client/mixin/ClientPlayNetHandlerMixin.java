package io.izzel.lightfall.client.mixin;

import io.izzel.lightfall.client.LightfallClient;
import io.izzel.lightfall.client.bridge.ClientLoginNetHandlerBridge;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.WorkingScreen;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.login.client.CCustomPayloadLoginPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetHandlerMixin {

    private static final byte[] RESET_ACK = "lightfall:ack".getBytes(StandardCharsets.UTF_8);

    @Shadow @Final private NetworkManager netManager;
    @Shadow private Minecraft client;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void lightfall$handleCustomPayload(SCustomPayloadPlayPacket packetIn, CallbackInfo ci) {
        if (packetIn.getChannelName().toString().equals("lightfall:reset")) {
            this.netManager.channel().flush();
            this.netManager.setConnectionState(ProtocolType.LOGIN);
            // ack
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(RESET_ACK));
            this.netManager.sendPacket(new CCustomPayloadLoginPacket(0x11FFA1, buffer));
            WorkingScreen screen = new WorkingScreen();
            ClientLoginNetHandler netHandler = new ClientLoginNetHandler(this.netManager, this.client, null, screen::displayLoadingString);
            ((ClientLoginNetHandlerBridge) netHandler).bridge$reusePlayHandler((ClientPlayNetHandler) (Object) this);
            this.netManager.setNetHandler(netHandler);
            LightfallClient.workQueue.add(() -> {
                if (this.client.world != null) {
                    this.client.world = null;
                }
                this.client.displayGuiScreen(screen);
                ((ThreadTaskExecutorInvoker) this.client).lightfall$dropTasks();
            });
            ci.cancel();
        }
    }
}
