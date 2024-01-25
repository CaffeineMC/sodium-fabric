package me.jellysquid.mods.sodium.client.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * Allows taking control of culling/occlusion yourself. This should only be done where other methods are not
 * possible, an example of this is forge/porting lib's obj loader that doesn't support configuring culling
 * manually.
 *
 * <p>
 * Example: <pre>
 * {@code
 * RenderBlockCallback.EVENT.register((state, adjacentBlockState, direction) -> {
 *  return (adjacentBlockState.is(MyModsBlocks.CAKE_BLOCK.get()) && adjacentBlockState.getValue(HORIZONTAL_AXIS) == state.getValue(HORIZONTAL_AXIS))
 * });
 * }
 * </pre>
 */
@FunctionalInterface
public interface RenderBlockCallback {
    Event<RenderBlockCallback> EVENT = EventFactory.createArrayBacked(RenderBlockCallback.class,
            (listeners) -> (state, adjacentBlockState, direction) -> {
                for (RenderBlockCallback listener : listeners) {
                    return listener.selfManageOcclusion(state, adjacentBlockState, direction);
                }
                return true;
            });

    boolean selfManageOcclusion(@NotNull BlockState state, BlockState adjacentBlockState, @NotNull Direction direction);
}
