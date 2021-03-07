package io.izzel.lightfall.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod("lightfallclient")
public class LightfallClient {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Queue<Runnable> workQueue = new ConcurrentLinkedQueue<>();

    public LightfallClient() {
        MinecraftForge.EVENT_BUS.register(this);
        NetworkRegistry.newSimpleChannel(
            new ResourceLocation("fml:handshake_reset"),
            () -> "1",
            s -> true,
            s -> true
        );
    }

    @SubscribeEvent
    public void onPreTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            while (!workQueue.isEmpty()) {
                workQueue.poll().run();
            }
        }
    }
}
