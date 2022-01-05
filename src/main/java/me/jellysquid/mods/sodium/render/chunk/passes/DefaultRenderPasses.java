package me.jellysquid.mods.sodium.render.chunk.passes;

import me.jellysquid.mods.sodium.opengl.types.RenderPipeline;
import me.jellysquid.mods.sodium.opengl.types.TranslucencyMode;

public class DefaultRenderPasses {
    public static final ChunkRenderPass SOLID = new ChunkRenderPass(RenderPipeline.defaults(), true, 0.0f);
    public static final ChunkRenderPass CUTOUT_MIPPED = new ChunkRenderPass(RenderPipeline.defaults(), true, 0.5f);
    public static final ChunkRenderPass CUTOUT = new ChunkRenderPass(RenderPipeline.defaults(), false, 0.1f);
    public static final ChunkRenderPass TRANSLUCENT = new ChunkRenderPass(RenderPipeline.builder()
            .setTranslucencyMode(TranslucencyMode.ENABLED)
            .build(), true, 0.0f);
    public static final ChunkRenderPass TRIPWIRE = new ChunkRenderPass(RenderPipeline.defaults(), true, 0.1f);
}
