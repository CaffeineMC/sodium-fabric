package me.jellysquid.mods.sodium.client.util.resource;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.profiler.Profiler;

public interface SinglePreparationResourceReloaderAccess<T> {
    T access$prepare(ResourceManager manager, Profiler profiler);

    void access$apply(T prepared, ResourceManager manager, Profiler profiler);
}
