package io.izzel.lightfall.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

public class LightfallHandshakeScreen extends Screen {

    private final Connection networkManager;
    private Component component;

    public LightfallHandshakeScreen(Connection netManager) {
        super(NarratorChatListener.NO_TITLE);
        this.networkManager = netManager;
    }

    @Override
    public void tick() {
        if (this.networkManager.isConnected()) {
            this.networkManager.tick();
        } else {
            this.networkManager.handleDisconnection();
            if (Minecraft.getInstance().screen == this) {
                Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
            }
        }
    }

    @Override
    protected void init() {
        this.addRenderableWidget(
            new Button(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20, CommonComponents.GUI_CANCEL,
                button -> {
                    this.networkManager.disconnect(new TranslatableComponent("connect.aborted"));
                    this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
                }
            )
        );
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public void render(@NotNull PoseStack poseStack, int p_96531_, int p_96532_, float p_96533_) {
        this.renderDirtBackground(0);
        drawCenteredString(poseStack, this.font, component != null ? component : new TranslatableComponent("connect.connecting"),
            this.width / 2, this.height / 2 - 50, 16777215);
        super.render(poseStack, p_96531_, p_96532_, p_96533_);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
