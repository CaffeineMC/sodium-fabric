package net.caffeinemc.mods.sodium.api.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ISelfHandleOcclusion {
    boolean selfManageOcclusion(@NotNull BlockState state, BlockState adjacentBlockState, @NotNull Direction direction);
}
