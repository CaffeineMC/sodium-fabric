package me.jellysquid.mods.sodium.render.chunk.passes;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.opengl.types.BlendFunction;
import me.jellysquid.mods.sodium.opengl.types.RenderState;

public class DefaultRenderPasses {
    public static final ChunkRenderPass SOLID = new ChunkRenderPass(RenderState.defaults(), true, 0.0f);
    public static final ChunkRenderPass CUTOUT_MIPPED = new ChunkRenderPass(RenderState.defaults(), true, 0.5f);
    public static final ChunkRenderPass CUTOUT = new ChunkRenderPass(RenderState.defaults(), false, 0.1f);
    public static final ChunkRenderPass TRANSLUCENT = new ChunkRenderPass(RenderState.builder()
            .setBlendFunction(BlendFunction.of(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA))
            .build(), true, 0.0f);
    public static final ChunkRenderPass TRIPWIRE = new ChunkRenderPass(RenderState.defaults(), true, 0.1f);
}
