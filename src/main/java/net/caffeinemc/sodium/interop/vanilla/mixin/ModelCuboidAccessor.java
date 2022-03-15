package net.caffeinemc.sodium.interop.vanilla.mixin;

import net.minecraft.client.model.ModelPart;

public interface ModelCuboidAccessor {
    ModelPart.Quad[] getQuads();
}
