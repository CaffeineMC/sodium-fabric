package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.resource.SinglePreparationResourceReloaderAccess;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SinglePreparationResourceReloader.class)
public abstract class MixinSinglePreparationResourceReloader<T> implements SinglePreparationResourceReloaderAccess<T> {
    @Shadow
    public abstract T prepare(ResourceManager manager, Profiler profiler);

    @Shadow
    public abstract void apply(T prepared, ResourceManager manager, Profiler profiler);

    @Override
    public T access$prepare(ResourceManager manager, Profiler profiler) {
        return this.prepare(manager, profiler);
    }

    @Override
    public void access$apply(T prepared, ResourceManager manager, Profiler profiler) {
        this.apply(prepared, manager, profiler);
    }
}
