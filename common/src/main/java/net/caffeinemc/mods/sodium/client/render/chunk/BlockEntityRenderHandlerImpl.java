package net.caffeinemc.mods.sodium.client.render.chunk;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderHandler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRenderHandlerImpl implements BlockEntityRenderHandler {
    @Override
    public <T extends BlockEntity> void addRenderPredicate(BlockEntityType<T> type, Predicate<? super T> predicate) {
        ExtendedBlockEntityType.addRenderPredicate(type, predicate);
    }

    @Override
    public <T extends BlockEntity> boolean removeRenderPredicate(BlockEntityType<T> type, Predicate<? super T> predicate) {
        return ExtendedBlockEntityType.removeRenderPredicate(type, predicate);
    }
}
