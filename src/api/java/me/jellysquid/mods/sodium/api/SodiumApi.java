package me.jellysquid.mods.sodium.api;

import me.jellysquid.mods.sodium.api.world.ISodiumWorldRenderer;
import net.minecraft.client.render.WorldRenderer;

public interface SodiumApi {
    static SodiumApi get() {
        return Impl.instance;
    }

    /**
     * Provides access to the implementation of {@link ISodiumWorldRenderer} for the given vanilla renderer.
     *
     * @param worldRenderer The vanilla renderer
     * @return The current instance
     * @throws IllegalStateException If the renderer has not yet been created
     */
    ISodiumWorldRenderer getWorldRenderer(WorldRenderer worldRenderer) throws IllegalStateException;

    final class Impl {
        static SodiumApi instance;
        private Impl() {}
    }
}
