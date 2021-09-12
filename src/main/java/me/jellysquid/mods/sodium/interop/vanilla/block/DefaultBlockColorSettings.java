package me.jellysquid.mods.sodium.interop.vanilla.block;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Set;

public class DefaultBlockColorSettings {
    private static final Set<Block> BLENDED_BLOCKS = new ReferenceOpenHashSet<>(Sets.newHashSet(
            Blocks.FERN, Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.GRASS, Blocks.TALL_GRASS,
            Blocks.GRASS_BLOCK, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.VINE, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON, Blocks.SUGAR_CANE));

    public static boolean isSmoothBlendingAvailable(Block block) {
        return BLENDED_BLOCKS.contains(block);
    }
}
