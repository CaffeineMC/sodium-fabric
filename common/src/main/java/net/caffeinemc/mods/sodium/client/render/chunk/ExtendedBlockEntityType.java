package net.caffeinemc.mods.sodium.client.render.chunk;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public interface ExtendedBlockEntityType<T extends BlockEntity> {
    Predicate<? super T>[] sodium$getRenderPredicates();

    void sodium$addRenderPredicate(Predicate<? super T> shouldAddRenderer);

    boolean sodium$removeRenderPredicate(Predicate<? super T> shouldAddRenderer);

    static <T extends BlockEntity> boolean shouldRender(BlockEntityType<? extends T> type, T entity) {
       Predicate<? super T>[] predicates = ((ExtendedBlockEntityType<T>) type).sodium$getRenderPredicates();

        for (int i = 0; i < predicates.length; i++) {
            if (!predicates[i].test(entity)) {
                return false;
            }
        }

        return true;
    }

    static <T extends BlockEntity> void addRenderPredicate(BlockEntityType<T> type, Predicate<? super T> predicate) {
        ((ExtendedBlockEntityType<T>) type).sodium$addRenderPredicate(predicate);
    }

    static <T extends BlockEntity> boolean removeRenderPredicate(BlockEntityType<T> type, Predicate<? super T> predicate) {
        return ((ExtendedBlockEntityType<T>) type).sodium$removeRenderPredicate(predicate);
    }
}
