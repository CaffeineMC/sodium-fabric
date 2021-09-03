package me.jellysquid.mods.sodium.client.model.light;

import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.interop.fabric.mesh.MutableQuadViewImpl;

/**
 * Light pipelines allow model quads for any location in the world to be lit using various backends, including fluids
 * and block entities. 
 */
public interface QuadLighter {
    void compute(MutableQuadViewImpl quad);

    QuadLightData getQuadLightData();
}
