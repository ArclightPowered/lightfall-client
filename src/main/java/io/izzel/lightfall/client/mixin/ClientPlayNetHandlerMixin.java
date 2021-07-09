package io.izzel.lightfall.client.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.STagsListPacket;
import net.minecraft.tags.ITagCollectionSupplier;
import net.minecraft.tags.TagRegistryManager;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayNetHandler.class)
public abstract class ClientPlayNetHandlerMixin implements IClientPlayNetHandler {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    private ITagCollectionSupplier networkTagManager;
    @Shadow
    private @Final
    NetworkManager netManager;
    @Shadow
    private Minecraft client;

    /**
     * @author
     */
    @Overwrite
    public void handleTags(STagsListPacket packetIn) {
        try {
            PacketThreadUtil.checkThreadAndEnqueue(packetIn, (ClientPlayNetHandler) (Object) this, this.client);
            ITagCollectionSupplier itagcollectionsupplier = packetIn.getTags();
            boolean vanillaConnection = net.minecraftforge.fml.network.NetworkHooks.isVanillaConnection(netManager);
            Multimap<ResourceLocation, ResourceLocation> multimap = vanillaConnection ? TagRegistryManager.validateTags(net.minecraftforge.common.ForgeTagHandler.withNoCustom(itagcollectionsupplier)) : TagRegistryManager.validateVanillaTags(itagcollectionsupplier);//Forge: If we are connecting to vanilla validate all tags to properly validate custom tags the client may "require", and if we are connecting to forge only validate the vanilla tag types as the custom tag types get synced in a separate packet so may still arrive

            if (!multimap.isEmpty()) {
                LOGGER.warn("Incomplete server tags, ignored. Missing: {}", (Object) multimap);
            }

            net.minecraftforge.common.ForgeTagHandler.resetCachedTagCollections(true, vanillaConnection);
            itagcollectionsupplier = ITagCollectionSupplier.reinjectOptionalTags(itagcollectionsupplier);
            this.networkTagManager = itagcollectionsupplier;
            if (!this.netManager.isLocalChannel()) {
                itagcollectionsupplier.updateTags();
            }

            this.client.getSearchTree(SearchTreeManager.TAGS).recalculate();
        } catch (Exception ignored) {
        }
    }
}