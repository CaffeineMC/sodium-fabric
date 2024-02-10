package net.caffeinemc.mods.sodium.client.world;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the {@link SodiumWorldRenderer} instance which is attached the global level renderer.
 */
public interface LevelRendererExtension {
    /**
     * @return The shared instance of {@link SodiumWorldRenderer}, if it has been initialized
     */
    @Nullable SodiumWorldRenderer sodium$getWorldRenderer();
}
