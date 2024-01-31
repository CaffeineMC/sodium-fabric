package me.jellysquid.mods.sodium.client.model.color;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.jellysquid.mods.sodium.client.model.color.interop.BlockColorsExtended;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;


// TODO: Make the registry a global somewhere that is only initialized once after content load
public class ColorProviderRegistry {
    private final Reference2ReferenceMap<Block, ColorProvider<BlockState>> blocks = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Fluid, ColorProvider<FluidState>> fluids = new Reference2ReferenceOpenHashMap<>();

    private final ReferenceSet<Block> overridenBlocks;

    public ColorProviderRegistry(BlockColors blockColors) {
        var providers = BlockColorsExtended.getProviders(blockColors);

        for (var entry : providers.reference2ReferenceEntrySet()) {
            this.blocks.put(entry.getKey(), DefaultColorProviders.adapt(entry.getValue()));
        }

        this.overridenBlocks = BlockColorsExtended.getOverridenVanillaBlocks(blockColors);

        this.installOverrides();
    }

    // TODO: Allow mods to install their own color resolvers here
    private void installOverrides() {
        this.registerBlocks(DefaultColorProviders.GrassColorProvider.BLOCKS,
                Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.SHORT_GRASS, Blocks.POTTED_FERN,
                Blocks.PINK_PETALS, Blocks.SUGAR_CANE, Blocks.LARGE_FERN, Blocks.TALL_GRASS);

        this.registerBlocks(DefaultColorProviders.FoliageColorProvider.BLOCKS,
                Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES, Blocks.VINE, Blocks.MANGROVE_LEAVES);

        this.registerBlocks(DefaultColorProviders.WaterColorProvider.BLOCKS,
                Blocks.WATER, Blocks.BUBBLE_COLUMN);

        this.registerFluids(DefaultColorProviders.WaterColorProvider.FLUIDS,
                Fluids.WATER, Fluids.FLOWING_WATER);
    }

    private void registerBlocks(ColorProvider<BlockState> resolver, Block... blocks) {
        for (var block : blocks) {
            // Do not register our resolver if the block is overriden
            if (this.overridenBlocks.contains(block))
                continue;
            this.blocks.put(block, resolver);
        }
    }

    private void registerFluids(ColorProvider<FluidState> resolver, Fluid... fluids) {
        for (var fluid : fluids) {
            this.fluids.put(fluid, resolver);
        }
    }

    public ColorProvider<BlockState> getColorProvider(Block block) {
        return this.blocks.get(block);
    }

    public ColorProvider<FluidState> getColorProvider(Fluid fluid) {
        return this.fluids.get(fluid);
    }
}
