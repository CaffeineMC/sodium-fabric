package me.jellysquid.mods.sodium.mixin.core.world.chunk;

import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PalettedContainer.class)
public interface PalettedContainerAccessor<T> {
    @Accessor
    PalettedContainer.Data<T> getData();
}
