package io.izzel.lightfall.client;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class LightfallConnector implements IMixinConnector {

    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.lightfall.json");
    }
}
