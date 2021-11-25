package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.util.math.JomlHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public record ChunkRenderMatrices(Matrix4f projection, Matrix4f modelView) {
    public static ChunkRenderMatrices from(MatrixStack stack) {
        MatrixStack.Entry entry = stack.peek();
        return new ChunkRenderMatrices(JomlHelper.copy(RenderSystem.getProjectionMatrix()), JomlHelper.copy(entry.getModel()));
    }
}
