package io.izzel.lightfall.client.mixin;

import net.minecraft.util.concurrent.ThreadTaskExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThreadTaskExecutor.class)
public interface ThreadTaskExecutorInvoker {

    @Invoker("dropTasks")
    void lightfall$dropTasks();
}
