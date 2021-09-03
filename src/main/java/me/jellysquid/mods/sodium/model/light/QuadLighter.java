package me.jellysquid.mods.sodium.model.light;

import me.jellysquid.mods.sodium.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.interop.fabric.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;

/**
 * Light pipelines allow model quads for any location in the world to be lit using various backends, including fluids
 * and block entities. 
 */
public interface QuadLighter {
    void compute(MutableQuadView quad);

    QuadLightData getQuadLightData();
}
