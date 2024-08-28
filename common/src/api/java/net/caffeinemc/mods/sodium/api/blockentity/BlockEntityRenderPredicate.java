package net.caffeinemc.mods.sodium.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.AvailableSince("0.6.0")
@FunctionalInterface
public interface BlockEntityRenderPredicate<T extends BlockEntity> {
    boolean shouldRender(BlockGetter blockGetter, BlockPos blockPos, T entity);
}
