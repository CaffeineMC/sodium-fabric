package me.jellysquid.mods.sodium.render.chunk.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.interop.vanilla.math.JomlHelper;
import org.joml.Matrix4f;

public record ChunkRenderMatrices(Matrix4f projection, Matrix4f modelView) {
    public static ChunkRenderMatrices from(PoseStack stack) {
        return new ChunkRenderMatrices(JomlHelper.copy(RenderSystem.getProjectionMatrix()), JomlHelper.copy(stack.last().pose()));
    }
}
