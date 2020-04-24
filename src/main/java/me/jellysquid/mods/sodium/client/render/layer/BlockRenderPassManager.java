package me.jellysquid.mods.sodium.client.render.layer;

import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import net.minecraft.client.render.RenderLayer;

public class BlockRenderPassManager {
    private final Reference2IntArrayMap<RenderLayer> mappingsId = new Reference2IntArrayMap<>();

    public BlockRenderPassManager() {
        this.mappingsId.defaultReturnValue(-1);
    }

    public int getRenderPassId(RenderLayer layer) {
        int pass = this.mappingsId.getInt(layer);

        if (pass < 0) {
            throw new NullPointerException("No render pass exists for layer: " + layer);
        }

        return pass;
    }

    private void addMapping(RenderLayer layer, BlockRenderPass type) {
        if (this.mappingsId.put(layer, type.ordinal()) >= 0) {
            throw new IllegalArgumentException("Layer target already defined for " + layer);
        }
    }

    public static BlockRenderPassManager vanilla() {
        BlockRenderPassManager mapper = new BlockRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), BlockRenderPass.SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), BlockRenderPass.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), BlockRenderPass.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), BlockRenderPass.TRANSLUCENT);

        return mapper;
    }

    public static BlockRenderPassManager consolidated() {
        BlockRenderPassManager mapper = new BlockRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), BlockRenderPass.SOLID_MIPPED);
        mapper.addMapping(RenderLayer.getCutoutMipped(), BlockRenderPass.SOLID_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), BlockRenderPass.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), BlockRenderPass.TRANSLUCENT);

        return mapper;
    }

    public BlockRenderPass getRenderPassForLayer(RenderLayer layer) {
        return this.getRenderPass(this.getRenderPassId(layer));
    }

    public BlockRenderPass getRenderPass(int i) {
        return BlockRenderPass.VALUES[i];
    }
}
