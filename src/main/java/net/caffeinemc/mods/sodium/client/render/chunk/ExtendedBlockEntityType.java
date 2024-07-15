package me.jellysquid.mods.sodium.client.render.chunk;

import java.util.function.Predicate;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

@SuppressWarnings("unchecked")
public interface ExtendedBlockEntityType<T extends BlockEntity> {
    Predicate<? super T> sodium$getRenderPredicate();

    void sodium$setRenderPredicate(Predicate<? super T> shouldAddRenderer);

    static <T extends BlockEntity> boolean shouldRender(BlockEntityType<? extends T> type, T entity) {
        return ((ExtendedBlockEntityType<T>) type).sodium$getRenderPredicate().test(entity);
    }

    static <T extends BlockEntity> void setRenderPredicate(BlockEntityType<T> type, Predicate<? super T> predicate) {
        ((ExtendedBlockEntityType<T>) type).sodium$setRenderPredicate(predicate);
    }
}
