package net.caffeinemc.mods.sodium.client.render.chunk;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderHandler;
import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderPredicate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRenderHandlerImpl implements BlockEntityRenderHandler {
    @Override
    public <T extends BlockEntity> void addRenderPredicate(BlockEntityType<T> type, BlockEntityRenderPredicate<T> predicate) {
        ExtendedBlockEntityType.addRenderPredicate(type, predicate);
    }

    @Override
    public <T extends BlockEntity> boolean removeRenderPredicate(BlockEntityType<T> type, BlockEntityRenderPredicate<T> predicate) {
        return ExtendedBlockEntityType.removeRenderPredicate(type, predicate);
    }
}
