package io.izzel.lightfall.client.mixin;

import net.minecraft.tags.ITag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.tags.TagRegistry$NamedTag")
abstract class NamedTagMixin<T> implements ITag.INamedTag<T> {
    @Shadow
    abstract ITag<T> getTag();

    public boolean contains(T element) {
        try{
            return this.getTag().contains(element);
        }catch(Exception e){
            return false;
        }
    }
}
