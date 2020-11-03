package me.jellysquid.mods.sodium.client.model.consumer;

import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.util.math.Matrix4f;

public interface LineVertexConsumer {
    void vertexLine(Matrix4f matrix, float x, float y, float z, int color);

    default void vertexLine(Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        this.vertexLine(matrix, x, y, z, ColorABGR.pack(r, g, b, a));
    }
}
