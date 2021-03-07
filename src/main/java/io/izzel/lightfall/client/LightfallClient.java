package io.izzel.lightfall.client;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod("lightfallclient")
public class LightfallClient {

    public static final Queue<Runnable> workQueue = new ConcurrentLinkedQueue<>();

    public LightfallClient() {
        MinecraftForge.EVENT_BUS.register(this);
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
