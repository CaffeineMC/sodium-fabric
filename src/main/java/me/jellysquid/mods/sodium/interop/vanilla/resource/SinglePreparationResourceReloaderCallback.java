package me.jellysquid.mods.sodium.interop.vanilla.resource;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.profiler.Profiler;

public class SinglePreparationResourceReloaderCallback<T> extends SinglePreparationResourceReloader<T> {
    private final SinglePreparationResourceReloaderAccess<T> parent;
    private final Runnable callback;

    @SuppressWarnings("unchecked")
    public SinglePreparationResourceReloaderCallback(SinglePreparationResourceReloader<T> parent, Runnable callback) {
        this.parent = (SinglePreparationResourceReloaderAccess<T>) parent;
        this.callback = callback;
    }

    @Override
    public T prepare(ResourceManager manager, Profiler profiler) {
        return this.parent.access$prepare(manager, profiler);
    }

    @Override
    public void apply(T prepared, ResourceManager manager, Profiler profiler) {
        this.parent.access$apply(prepared, manager, profiler);
        this.callback.run();
    }
}
