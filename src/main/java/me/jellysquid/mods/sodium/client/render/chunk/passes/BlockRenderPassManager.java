package me.jellysquid.mods.sodium.client.render.chunk.passes;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps vanilla render layers to render passes used by Sodium. This provides compatibility with the render layers already
 * used by the base game.
 */
public class BlockRenderPassManager {
    private final Map<Block, BlockRenderPass> blocks;
    private final Map<Fluid, BlockRenderPass> fluids;

    private final BlockRenderPass defaultPass;

    private final Set<BlockRenderPass> totalPasses;

    public BlockRenderPassManager(Map<Block, BlockRenderPass> blocks,
                                  Map<Fluid, BlockRenderPass> fluids,
                                  BlockRenderPass defaultPass) {
        this.blocks = blocks;
        this.fluids = fluids;
        this.defaultPass = defaultPass;

        this.totalPasses = Stream.of(blocks.values(), fluids.values(), Collections.singletonList(defaultPass))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    public BlockRenderPass getRenderPass(Block block) {
        return getRenderPass(this.blocks, block);
    }

    public BlockRenderPass getRenderPass(Fluid fluid) {
        return getRenderPass(this.fluids, fluid);
    }

    private <T> BlockRenderPass getRenderPass(Map<T, BlockRenderPass> registry, T key) {
        BlockRenderPass pass = registry.get(key);

        if (pass == null) {
            return this.defaultPass;
        }

        return pass;
    }

    /**
     * Creates a set of render pass mappings to vanilla render layers which closely mirrors the rendering
     * behavior of vanilla.
     */
    public static BlockRenderPassManager create() {
        Map<Block, BlockRenderPass> blocks = new Reference2ObjectOpenHashMap<>();
        Map<Fluid, BlockRenderPass> fluids = new Reference2ObjectOpenHashMap<>();

        for (Map.Entry<Block, RenderLayer> entry : RenderLayers.BLOCKS.entrySet()) {
            Block block = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getCutoutMipped()) {
                pass = DefaultBlockRenderPasses.CUTOUT_MIPPED;
            } else if (layer == RenderLayer.getCutout()) {
                pass = DefaultBlockRenderPasses.CUTOUT;
            } else if (layer == RenderLayer.getTranslucent()) {
                pass = DefaultBlockRenderPasses.TRANSLUCENT;
            } else if (layer == RenderLayer.getTripwire()) {
                pass = DefaultBlockRenderPasses.TRIPWIRE;
            } else {
                pass = DefaultBlockRenderPasses.SOLID;
            }

            blocks.put(block, pass);
        }

        for (Map.Entry<Fluid, RenderLayer> entry : RenderLayers.FLUIDS.entrySet()) {
            Fluid fluid = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getTranslucent()) {
                pass = DefaultBlockRenderPasses.TRANSLUCENT;
            } else if (layer == RenderLayer.getSolid()) {
                pass = DefaultBlockRenderPasses.SOLID;
            } else {
                throw new IllegalStateException("Unknown render layer for fluid: " + fluid);
            }

            fluids.put(fluid, pass);
        }

        return new BlockRenderPassManager(blocks, fluids, DefaultBlockRenderPasses.SOLID);
    }

    public Iterable<BlockRenderPass> getRenderPasses() {
        return this.totalPasses;
    }
}
