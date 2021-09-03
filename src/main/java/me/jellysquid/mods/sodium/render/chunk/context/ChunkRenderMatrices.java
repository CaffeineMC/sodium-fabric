package me.jellysquid.mods.sodium.render.chunk.context;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.interop.vanilla.matrix.MatrixConverter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public record ChunkRenderMatrices(Matrix4f projection, Matrix4f modelView) {
    public static ChunkRenderMatrices from(MatrixStack stack) {
        MatrixStack.Entry entry = stack.peek();
        return new ChunkRenderMatrices(MatrixConverter.copy(RenderSystem.getProjectionMatrix()), MatrixConverter.copy(entry.getModel()));
    }
}
