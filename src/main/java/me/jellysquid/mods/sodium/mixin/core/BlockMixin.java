package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public class BlockMixin implements BlockOcclusionCache.ISelfHandleOcclusion {
    @Override
    public boolean sodium$selfManageOcclusion(@NotNull BlockState state, BlockState adjacentBlockState, @NotNull Direction direction) {
        return false;
    }
}
