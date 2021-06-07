package me.jellysquid.mods.sodium.api.world;

import net.minecraft.client.render.WorldRenderer;

/**
 * Provides an extension to vanilla's {@link WorldRenderer}.
 */
public interface ISodiumWorldRenderer {
    /**
     * Blocks until all dirty chunks have been fully (re)built and uploaded.
     * Must be called from the main thread.
     *
     * Primary use case being "screenshot" mods which want to force all chunks to be loaded before taking a picture.
     *
     * For maximum effect, this should be called between terrain setup (setupTerrain) and rendering. Also note that
     * new chunk may become visible as a result of this call but these will not be marked dirty and as such might not
     * be updated until setupTerrain is called again.
     *
     * @return True if any work was done. This can serve as an indication that new chunks may have become visible.
     */
    boolean updateAllDirtyChunks();
}
