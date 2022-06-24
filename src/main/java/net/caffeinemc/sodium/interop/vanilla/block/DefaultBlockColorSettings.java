package net.caffeinemc.sodium.interop.vanilla.block;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;

import java.util.Set;

public class DefaultBlockColorSettings {
    private static final Set<Block> BLENDED_BLOCKS = new ReferenceOpenHashSet<>(Sets.newHashSet(
            Blocks.FERN, Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.GRASS, Blocks.TALL_GRASS,
            Blocks.GRASS_BLOCK, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES, Blocks.AZALEA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.VINE, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON, Blocks.SUGAR_CANE));

    private static final Set<Fluid> BLENDED_FLUIDS = new ReferenceOpenHashSet<>(Sets.newHashSet(
            Fluids.EMPTY, Fluids.WATER, Fluids.FLOWING_WATER, Fluids.LAVA, Fluids.FLOWING_LAVA));

    /**
     * Gets a value indicating if the specified block is registered for smooth blending.
     */
    public static boolean isSmoothBlendingAvailable(Block block) {
        return BLENDED_BLOCKS.contains(block);
    }

    /**
     * Gets a value indicating if the specified fluid is registered for smooth blending.
     */
    public static boolean isSmoothBlendingAvailable(Fluid fluid) {
        return BLENDED_FLUIDS.contains(fluid);
    }

    /**
     * Registers a block for smooth blending.
     */
    @SuppressWarnings("unused")
    public static void registerForBlending(Block block) {
        BLENDED_BLOCKS.add(block);
    }

    /**
     * Registers a fluid for smooth blending.
     */
    @SuppressWarnings("unused")
    public static void registerForBlending(Fluid fluid) {
        BLENDED_FLUIDS.add(fluid);
    }
}