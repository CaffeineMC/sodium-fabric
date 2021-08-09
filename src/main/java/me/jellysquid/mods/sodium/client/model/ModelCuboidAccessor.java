package me.jellysquid.mods.sodium.client.model;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelCuboidAccessor {
    ModelPart.Polygon[] getQuads();
}
