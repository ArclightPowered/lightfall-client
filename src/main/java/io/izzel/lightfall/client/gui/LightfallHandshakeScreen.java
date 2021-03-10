package io.izzel.lightfall.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.WorkingScreen;
import net.minecraft.network.NetworkManager;

public class LightfallHandshakeScreen extends WorkingScreen {

    private final NetworkManager networkManager;

    public LightfallHandshakeScreen(NetworkManager netManager) {
        this.networkManager = netManager;
    }

    @Override
    public void tick() {
        if (this.networkManager.isChannelOpen()) {
            this.networkManager.tick();
        } else {
            this.networkManager.handleDisconnection();
            if (Minecraft.getInstance().currentScreen == this) {
                Minecraft.getInstance().displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
            }
        }
    }
}
