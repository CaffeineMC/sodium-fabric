package me.jellysquid.mods.sodium.render.entity.renderer;

import me.jellysquid.mods.sodium.render.entity.data.ModelBakingData;
import me.jellysquid.mods.thingl.device.RenderDevice;

public interface EntityRenderer {

    void render(RenderDevice device, ModelBakingData modelBakingData);

    void delete();
}
