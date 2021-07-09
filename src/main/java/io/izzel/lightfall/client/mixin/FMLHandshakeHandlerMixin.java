package io.izzel.lightfall.client.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.netty.util.AttributeKey;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.loading.AdvancedLogMessageAdapter;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import net.minecraftforge.fml.network.FMLHandshakeMessages;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static net.minecraftforge.registries.ForgeRegistry.REGISTRIES;

@Mixin(value = FMLHandshakeHandler.class, remap = false)
public class FMLHandshakeHandlerMixin {
    @Shadow
    @Final
    static Marker FMLHSMARKER;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    private Map<ResourceLocation, ForgeRegistry.Snapshot> registrySnapshots;
    @Shadow
    private Set<ResourceLocation> registriesToReceive;
    @Shadow
    @Final
    private NetworkManager manager;

    /**
     * @author VeroFess
     */
    @Overwrite
    private boolean handleRegistryLoading(final Supplier<NetworkEvent.Context> contextSupplier) {
        // We use a countdown latch to suspend the network thread pending the client thread processing the registry data
        AtomicBoolean successfulConnection = new AtomicBoolean(false);
        CountDownLatch block = new CountDownLatch(1);
        contextSupplier.get().enqueueWork(() -> {
            LOGGER.debug(FMLHSMARKER, "Injecting registry snapshot from server.");
            final Multimap<ResourceLocation, ResourceLocation> missingData = GameData.injectSnapshot(RegistryManager.ACTIVE.takeSnapshot(false), false, false);
            LOGGER.debug(FMLHSMARKER, "Snapshot injected.");
            successfulConnection.set(true);
            block.countDown();
        });
        LOGGER.debug(FMLHSMARKER, "Waiting for registries to load.");
        try {
            block.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        if (successfulConnection.get()) {
            LOGGER.debug(FMLHSMARKER, "Registry load complete, continuing handshake.");
        } else {
            LOGGER.error(FMLHSMARKER, "Failed to load registry, closing connection.");
            this.manager.closeChannel(new StringTextComponent("Failed to synchronize registry data from server, closing connection"));
        }
        return successfulConnection.get();
    }

    /**
     * @author VeroFess
     */
    @Overwrite
    void handleServerModListOnClient(FMLHandshakeMessages.S2CModList serverModList, Supplier<NetworkEvent.Context> c) {
        boolean accepted = false;

        LOGGER.debug(FMLHSMARKER, "Logging into server with mod list [{}]", String.join(", ", serverModList.getModList()));

        try {
            Class<?> networkRegistryClass = Class.forName("net.minecraftforge.fml.network.NetworkRegistry");
            Method validateClientChannels = networkRegistryClass.getDeclaredMethod("validateClientChannels", Map.class);
            validateClientChannels.setAccessible(true);
            accepted = (Boolean) validateClientChannels.invoke(null, serverModList.getChannels());
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        c.get().setPacketHandled(true);
        if (!accepted) {
            LOGGER.warn(FMLHSMARKER, "Mismatched mod list, but continue.");
        }

        try {
            Class<?> fmlNetworkConstantsClass = Class.forName("net.minecraftforge.fml.network.FMLNetworkConstants");
            Field handshakeChannel = fmlNetworkConstantsClass.getDeclaredField("handshakeChannel");
            handshakeChannel.setAccessible(true);
            ((SimpleChannel) handshakeChannel.get(null)).reply(new FMLHandshakeMessages.C2SModListReply(), c.get());
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        LOGGER.debug(FMLHSMARKER, "Accepted server connection");
        c.get().getNetworkManager().channel().attr(AttributeKey.valueOf("fml:netversion")).set(FMLNetworkConstants.NETVERSION);
        this.registriesToReceive = new HashSet<>(serverModList.getRegistries());
        this.registrySnapshots = Maps.newHashMap();
        LOGGER.debug(REGISTRIES, "Expecting {} registries: {}", () -> this.registriesToReceive.size(), () -> this.registriesToReceive);
    }
}
