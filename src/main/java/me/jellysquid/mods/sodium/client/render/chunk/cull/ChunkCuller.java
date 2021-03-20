package me.jellysquid.mods.sodium.client.render.chunk.cull;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import net.minecraft.client.render.Camera;

public interface ChunkCuller {
    IntArrayList computeVisible(Camera camera, FrustumExtended frustum, int frame, boolean spectator);

    void onSectionLoaded(int x, int y, int z, int sectionId);

    void onSectionStateChanged(int sectionId, ChunkRenderData renderData);

    void onSectionUnloaded(int sectionId);

    boolean isSectionVisible(int sectionId);
}
