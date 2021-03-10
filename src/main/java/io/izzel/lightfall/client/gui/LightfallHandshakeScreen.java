package io.izzel.lightfall.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.WorkingScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.TranslationTextComponent;

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

    @Override
    protected void init() {
        this.displaySavingString(new TranslationTextComponent("connect.connecting"));
        this.addButton(
            new Button(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20, DialogTexts.GUI_CANCEL,
                button -> {
                    this.networkManager.closeChannel(new TranslationTextComponent("connect.aborted"));
                    this.minecraft.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
                }
            )
        );
    }
}
