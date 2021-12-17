package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.util.math.Vec3i;

public interface ModelQuadSink {
    void add(Vec3i pos, ModelQuadView quad, ModelQuadFacing facing);
}
