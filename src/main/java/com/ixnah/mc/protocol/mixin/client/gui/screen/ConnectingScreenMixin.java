package com.ixnah.mc.protocol.mixin.client.gui.screen;

import com.ixnah.mc.protocol.UriServerAddress;
import com.ixnah.mc.protocol.bridge.CustomProtocolBridge;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.network.login.ClientLoginNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.client.CLoginStartPacket;
import net.minecraft.util.DefaultUncaughtExceptionHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ixnah.mc.protocol.CustomProtocol.URI_REGEX;
import static java.util.Objects.requireNonNull;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 22:47
 */
@Mixin(ConnectingScreen.class)
public abstract class ConnectingScreenMixin extends Screen {
    @Shadow
    @Final
    private static final AtomicInteger CONNECTION_ID = new AtomicInteger(0);
    @Shadow
    @Final
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow
    private NetworkManager networkManager;
    @Shadow
    private boolean cancel;
    @Shadow
    @Final
    private Screen previousGuiScreen;

    private URI serverUri;

    @Shadow
    private void func_209514_a(ITextComponent p_209514_1_) {
    }

    protected ConnectingScreenMixin(ITextComponent titleIn) {
        super(titleIn);
    }

    @Redirect(method = "<init>(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ServerAddress;getIP()Ljava/lang/String;"))
    private String getServerIP(ServerAddress serverAddress) {
        if (serverAddress instanceof UriServerAddress) {
            return ((UriServerAddress) serverAddress).getAddress().toString();
        }
        return serverAddress.getIP();
    }

    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ConnectingScreen;connect(Ljava/lang/String;I)V"))
    private void redirectConnect(ConnectingScreen connectingScreen, String ip, int port) {
        if (ip.matches(URI_REGEX)) {
            try {
                UriServerAddress uriServerAddress = new UriServerAddress(new URI(ip));
                serverUri = uriServerAddress.getAddress();
                this.connect(uriServerAddress.getIP(), uriServerAddress.getPort());
                return;
            } catch (URISyntaxException ignored) {
            }
        }
        this.connect(ip, port);
    }

    /**
     * @author 寒兮
     * @reason Mixin不支持非static class
     * static class无法访问外部类的变量
     */
    @Overwrite
    private void connect(final String ip, final int port) {
        requireNonNull(this.minecraft, "Minecraft must not be null!");
        LOGGER.info("Connecting to {}, {}", ip, port);
        Thread thread = new Thread(() -> {
            InetAddress inetaddress = null;

            try {
                if (this.cancel) {
                    return;
                }

                inetaddress = InetAddress.getByName(ip);
                this.networkManager = NetworkManager.createNetworkManagerAndConnect(inetaddress, port, this.minecraft.gameSettings.isUsingNativeTransport());
                ((CustomProtocolBridge) this.networkManager).setServerUri(serverUri);
                this.networkManager.setNetHandler(new ClientLoginNetHandler(this.networkManager, this.minecraft, this.previousGuiScreen, this::func_209514_a));
                this.networkManager.sendPacket(new CHandshakePacket(ip, port, ProtocolType.LOGIN));
                this.networkManager.sendPacket(new CLoginStartPacket(this.minecraft.getSession().getProfile()));
            } catch (UnknownHostException unknownhostexception) {
                if (this.cancel) {
                    return;
                }

                LOGGER.error("Couldn't connect to server", unknownhostexception);
                this.minecraft.execute(() -> this.minecraft.displayGuiScreen(new DisconnectedScreen(this.previousGuiScreen, DialogTexts.CONNECTION_FAILED, new TranslationTextComponent("disconnect.genericReason", "Unknown host"))));
            } catch (Exception exception) {
                if (this.cancel) {
                    return;
                }

                LOGGER.error("Couldn't connect to server", exception);
                String s = inetaddress == null ? exception.toString() : exception.toString().replaceAll(inetaddress + ":" + port, "");
                this.minecraft.execute(() -> this.minecraft.displayGuiScreen(new DisconnectedScreen(this.previousGuiScreen, DialogTexts.CONNECTION_FAILED, new TranslationTextComponent("disconnect.genericReason", s))));
            }

        }, "Server Connector #" + CONNECTION_ID.incrementAndGet());
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }
}
