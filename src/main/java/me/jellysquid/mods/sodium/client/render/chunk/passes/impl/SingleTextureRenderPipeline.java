package me.jellysquid.mods.sodium.client.render.chunk.passes.impl;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.WorldRenderPhase;
import net.minecraft.util.Identifier;

public class SingleTextureRenderPipeline {
    public static final Identifier SOLID_PASS = new Identifier("sodium", "single_texture/solid");
    public static final Identifier SOLID_MIPPED_PASS = new Identifier("sodium", "single_texture/solid_mipped");
    public static final Identifier TRANSLUCENT_MIPPED_PASS = new Identifier("sodium", "single_texture/translucent_mipped");

    public static BlockRenderPassManager create() {
        BlockRenderPassManager registry = new BlockRenderPassManager();
        registry.add(WorldRenderPhase.OPAQUE, SOLID_PASS, SolidRenderPass::new, BlockLayer.SOLID);
        registry.add(WorldRenderPhase.OPAQUE, SOLID_MIPPED_PASS, SolidRenderPass::new, BlockLayer.SOLID_MIPPED);
        registry.add(WorldRenderPhase.TRANSLUCENT, TRANSLUCENT_MIPPED_PASS, TranslucentRenderPass::new, BlockLayer.TRANSLUCENT_MIPPED);

        return registry;
    }
}
