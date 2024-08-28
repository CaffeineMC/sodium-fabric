package net.caffeinemc.mods.sodium.api.blockentity;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.AvailableSince("0.6.0")
public interface BlockEntityRenderHandler {
    BlockEntityRenderHandler INSTANCE = DependencyInjection.load(BlockEntityRenderHandler.class,
            "net.caffeinemc.mods.sodium.client.render.chunk.BlockEntityRenderHandlerImpl");

    static BlockEntityRenderHandler instance() {
        return INSTANCE;
    }

    /**
     * Adds a predicate to determine if a block entity should be rendered.
     *
     * <p>Upon chunk bake, block entities of the given type will have {@code shouldRender} evaluated.
     * <br>If <b>all predicates</b> returns {@code true} (and the block entity has a renderer), the block entity will be
     * added to the chunk for future rendering.</p>
     * @param type The block entity type to associate the given predicate with.
     * @param shouldRender The predicate for the block entity to evaluate.
     */
    <T extends BlockEntity> void addRenderPredicate(BlockEntityType<T> type, BlockEntityRenderPredicate<T> shouldRender);

    /**
     * Removes a predicate added by {@code addRenderPredicate}. <b>It must be the same object that was added.</b>
     *
     * @param type The block entity type to associate the given predicate with.
     * @param shouldRender The predicate to remove.
     * @return If the predicate existed and was removed.
     */
    <T extends BlockEntity> boolean removeRenderPredicate(BlockEntityType<T> type, BlockEntityRenderPredicate<T> shouldRender);
}
