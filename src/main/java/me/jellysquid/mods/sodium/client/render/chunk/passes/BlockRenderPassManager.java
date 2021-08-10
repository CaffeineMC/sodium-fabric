package me.jellysquid.mods.sodium.client.render.chunk.passes;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.resource.ResourceLoader;
import me.jellysquid.mods.sodium.client.resource.shader.json.RenderPassJson;
import me.jellysquid.mods.sodium.client.resource.shader.json.ShaderJson;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
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
        BlockRenderPass cutoutMipped = createRenderPass(new Identifier("sodium", "passes/block_cutout_mipped.json"));
        BlockRenderPass cutout = createRenderPass(new Identifier("sodium", "passes/block_cutout.json"));
        BlockRenderPass solid = createRenderPass(new Identifier("sodium", "passes/block_solid.json"));
        BlockRenderPass translucent = createRenderPass(new Identifier("sodium", "passes/block_translucent.json"));
        BlockRenderPass tripwire = createRenderPass(new Identifier("sodium", "passes/block_tripwire.json"));

        Map<Block, BlockRenderPass> blocks = new Reference2ObjectOpenHashMap<>();
        Map<Fluid, BlockRenderPass> fluids = new Reference2ObjectOpenHashMap<>();

        for (Map.Entry<Block, RenderLayer> entry : RenderLayers.BLOCKS.entrySet()) {
            Block block = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getCutoutMipped()) {
                pass = cutoutMipped;
            } else if (layer == RenderLayer.getCutout()) {
                pass = cutout;
            } else if (layer == RenderLayer.getTranslucent()) {
                pass = translucent;
            } else if (layer == RenderLayer.getTripwire()) {
                pass = tripwire;
            } else {
                pass = solid;
            }

            blocks.put(block, pass);
        }

        for (Map.Entry<Fluid, RenderLayer> entry : RenderLayers.FLUIDS.entrySet()) {
            Fluid fluid = entry.getKey();
            RenderLayer layer = entry.getValue();

            BlockRenderPass pass;

            if (layer == RenderLayer.getTranslucent()) {
                pass = translucent;
            } else if (layer == RenderLayer.getSolid()) {
                pass = solid;
            } else {
                throw new IllegalStateException("Unknown render layer for fluid: " + fluid);
            }

            fluids.put(fluid, pass);
        }

        return new BlockRenderPassManager(blocks, fluids, solid);
    }

    public Iterable<BlockRenderPass> getRenderPasses() {
        return this.totalPasses;
    }

    public List<BlockRenderPass> getSolidRenderPasses() {
        return this.totalPasses.stream().filter(BlockRenderPass::isSolid).toList();
    }

    public List<BlockRenderPass> getTranslucentRenderPasses() {
        return this.totalPasses.stream().filter(BlockRenderPass::isTranslucent).toList();
    }

    private static final Gson GSON = new Gson();

    private static BlockRenderPass createRenderPass(Identifier id) {
        RenderPassJson json;

        try (InputStream in = ResourceLoader.EMBEDDED.open(id)) {
            json = GSON.fromJson(new InputStreamReader(in), RenderPassJson.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read render pass specification", e);
        }

        return new BlockRenderPass(getLayerByName(json.getLayer()), json.isTranslucent(),
                createShaderInfo(json.getShader("vertex")),
                createShaderInfo(json.getShader("fragment")));
    }

    private static RenderPassShader createShaderInfo(ShaderJson json) {
        ShaderConstants.Builder constants = ShaderConstants.builder();

        if (json.getConstants() != null) {
            for (Map.Entry<String, String> entry : json.getConstants().entrySet()) {
                constants.add(entry.getKey(), entry.getValue());
            }
        }

        return new RenderPassShader(new Identifier(json.getSource()), constants.build());
    }

    private static RenderLayer getLayerByName(String name) {
        return switch (name) {
            case "minecraft:solid" -> RenderLayer.getSolid();
            case "minecraft:translucent" -> RenderLayer.getTranslucent();
            case "minecraft:cutout" -> RenderLayer.getCutout();
            case "minecraft:cutout_mipped" -> RenderLayer.getCutoutMipped();
            case "minecraft:tripwire" -> RenderLayer.getTripwire();
            default -> throw new IllegalArgumentException("Unknown layer name: " + name);
        };
    }
}
