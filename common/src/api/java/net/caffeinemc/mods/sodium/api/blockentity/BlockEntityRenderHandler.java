package net.caffeinemc.mods.sodium.api.blockentity;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface BlockEntityRenderHandler {
    BlockEntityRenderHandler INSTANCE = DependencyInjection.load(BlockEntityRenderHandler.class,
            "net.caffeinemc.mods.sodium.client.render.chunk.BlockEntityRenderHandlerImpl");

    static BlockEntityRenderHandler instance() {
        return INSTANCE;
    }

    /**
     * Set a predicate to determine if a block entity should be rendered.
     *
     * <p>Upon chunk bake, block entities of the given type will have {@code shouldRender} evaluated.
     * <br>If it returns {@code true} (and the block entity has a renderer), the block entity will be
     * added to the chunk for future rendering.</p>
     * @param type The block entity type to associate the given predicate with.
     * @param shouldRender The predicate for sodium to evaluate.
     */
    <T extends BlockEntity> void setRenderPredicate(BlockEntityType<T> type, Predicate<? super T> shouldRender);
}
