package me.jellysquid.mods.sodium.client.render.layer;

import it.unimi.dsi.fastutil.objects.Reference2IntArrayMap;
import net.minecraft.client.render.RenderLayer;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
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

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static BlockRenderPassManager vanilla() {
        BlockRenderPassManager mapper = new BlockRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), BlockRenderPass.SOLID);
        mapper.addMapping(RenderLayer.getCutoutMipped(), BlockRenderPass.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutout(), BlockRenderPass.CUTOUT);
        mapper.addMapping(RenderLayer.getTranslucent(), BlockRenderPass.TRANSLUCENT);

        return mapper;
    }

    /**
     * Creates a consolidated set of render pass mappings to vanilla render layers. Namely, this merges the
     * solid and cutout-mipped layers into the same render pass in order to reduce the amount of draw calls needed
     * to render things such as foliage.
     *
     * This optimization prevents the usage of a hack which allowed leaf blocks to be made opaque when the fast graphics
     * option was selected as it would simply change the render layer to something that discarded the alpha channel. In
     * older versions of the game, this option would actually make a meaningful improvement as otherwise there would
     * be lots of extra geometry and visible fragments that couldn't be culled.
     *
     * However, in recent versions of the game, mip-mapping allows the graphics card to switch to a more opaque texture
     * as it's made further away from the player, which in turn allows it to discard fragments behind leaf blocks more
     * quickly.
     *
     * This hack also doesn't seem to work correctly in 1.13+ anymore as the game still builds geometry for inner leaf
     * block faces even though they're fully occluded with the opaque texture enabled. Given that the biggest bottleneck
     * on the graphics side of things is vertex processing rather than fragment processing nowadays, consolidating these
     * render calls will pretty much always yield a more significant improvement than making the leaf blocks opaque.
     */
    public static BlockRenderPassManager consolidated() {
        BlockRenderPassManager mapper = new BlockRenderPassManager();
        mapper.addMapping(RenderLayer.getSolid(), BlockRenderPass.CUTOUT_MIPPED);
        mapper.addMapping(RenderLayer.getCutoutMipped(), BlockRenderPass.CUTOUT_MIPPED);
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
