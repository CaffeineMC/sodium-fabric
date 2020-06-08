package me.jellysquid.mods.sodium.client.render.backends;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;

public abstract class AbstractChunkRenderBackend<T extends ChunkGraphicsState> implements ChunkRenderBackend<T> {
    protected void begin(MatrixStack matrixStack) {
        RenderSystem.pushMatrix();
        matrixStack.push();
    }

    protected void end(MatrixStack matrixStack) {
        matrixStack.pop();
        RenderSystem.popMatrix();
    }
}
